package smartcharge.payment;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartcharge.charging.ChargingRequest;
import smartcharge.charging.ChargingRequestRepository;
import smartcharge.user.User;
import smartcharge.user.UserRepository;
import smartcharge.scheduler.ChargingSchedulerService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingPaymentService {
    public static final double RATE_PER_KWH = 8.50;
    private final BookingPaymentRepository payments;
    private final BookingPaymentAttemptRepository attempts;
    private final ChargingRequestRepository requests;
    private final UserRepository users;
    private final ChargingSchedulerService scheduler;

    public BookingPaymentService(BookingPaymentRepository payments, BookingPaymentAttemptRepository attempts,
                                 ChargingRequestRepository requests, UserRepository users,
                                 ChargingSchedulerService scheduler) {
        this.payments = payments; this.attempts = attempts; this.requests = requests; this.users = users;
        this.scheduler = scheduler;
    }

    @Transactional(readOnly = true)
    public CheckoutView getCheckout(Long requestId, Authentication auth) {
        ChargingRequest request = ownedRequest(requestId, auth);
        return view(request, payments.findByRequestId(requestId).orElse(null));
    }

    @Transactional
    public CheckoutView attempt(Long requestId, PaymentAttempt input, Authentication auth) {
        if (input == null || input.method() == null || !List.of("UPI", "CARD", "WALLET").contains(input.method()))
            throw new IllegalArgumentException("Choose UPI, card, or wallet for this demo payment");
        if (input.outcome() == null || !List.of("SUCCESS", "FAILURE").contains(input.outcome()))
            throw new IllegalArgumentException("Choose a successful or failed demo outcome");
        ChargingRequest request = ownedRequest(requestId, auth);
        if (!"PAYMENT_PENDING".equalsIgnoreCase(request.getStatus()))
            throw new IllegalArgumentException("This booking is no longer awaiting payment. A failed booking must be placed again.");

        double energy = energyKwh(request);
        double amount = roundMoney(energy * RATE_PER_KWH);
        LocalDateTime now = LocalDateTime.now();
        boolean success = "SUCCESS".equals(input.outcome());
        String status = success ? "PAID" : "FAILED";
        String reference = success ? "DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : null;
        BookingPayment payment = payments.findByRequestId(requestId).orElseGet(() -> new BookingPayment(request, amount));
        payment.setStatus(status);
        payment.setPaymentMethod(input.method());
        payment.setPaymentReference(reference);
        payment.setPaymentTime(now);
        payments.save(payment);
        attempts.save(new BookingPaymentAttempt(request, amount, status, input.method(), reference, now));

        // Rollback policy for the demo: a failed advance payment cancels this
        // unallocated request. No charger/session exists to release or refund.
        request.setStatus(success ? "PENDING" : "CANCELLED");
        requests.save(request);
        if (success && Boolean.TRUE.equals(request.getEmergencyRequested())) {
            scheduler.processEmergencyIfChargerAvailable(requestId);
        }
        return view(request, payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentService.TransactionView> history(Authentication auth) {
        User user = currentUser(auth);
        return attempts.findForUser(user.getId()).stream()
                .map(a -> new PaymentService.TransactionView(a.getId(), null, a.getAmount(), a.getStatus(),
                        a.getPaymentMethod(), a.getPaymentReference(), a.getAttemptedAt(), a.getRequest().getId()))
                .toList();
    }

    private ChargingRequest ownedRequest(Long id, Authentication auth) {
        ChargingRequest request = requests.findById(id).orElseThrow(() -> new IllegalArgumentException("Booking request not found"));
        if (!request.getUser().getId().equals(currentUser(auth).getId()))
            throw new AccessDeniedException("This booking does not belong to your account");
        return request;
    }
    private User currentUser(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow(() -> new AccessDeniedException("User account not found"));
    }
    private CheckoutView view(ChargingRequest r, BookingPayment p) {
        double energy = energyKwh(r);
        String status = p == null ? ("PAYMENT_PENDING".equalsIgnoreCase(r.getStatus()) ? "AWAITING_PAYMENT" : r.getStatus()) : p.getStatus();
        return new CheckoutView(r.getId(), energy, RATE_PER_KWH, roundMoney(energy * RATE_PER_KWH),
                r.getEstimatedMinutes(), r.getEstimatedWaitMinutes(), status,
                p == null ? null : p.getPaymentMethod(), p == null ? null : p.getPaymentReference(),
                r.getStatus());
    }
    private double energyKwh(ChargingRequest r) {
        if (r.getVehicle() == null || r.getVehicle().getBatteryCapacity() == null) return 0;
        return r.getVehicle().getBatteryCapacity() * (r.getTargetBattery() - r.getCurrentBattery()) / 100.0;
    }
    private static double roundMoney(double amount) { return Math.round(amount * 100) / 100.0; }
    public record PaymentAttempt(String method, String outcome) {}
    public record CheckoutView(Long requestId, Double energyKwh, Double ratePerKwh, Double amount,
                               Double chargingMinutes, Double waitMinutes, String paymentStatus,
                               String paymentMethod, String paymentReference, String requestStatus) {}
}
