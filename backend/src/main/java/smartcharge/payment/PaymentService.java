package smartcharge.payment;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartcharge.charging.ChargingSession;
import smartcharge.charging.ChargingSessionRepository;
import smartcharge.user.User;
import smartcharge.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    public static final double RATE_PER_KWH = 8.50;
    private final PaymentRepository payments;
    private final PaymentTransactionRepository transactions;
    private final ChargingSessionRepository sessions;
    private final UserRepository users;

    public PaymentService(PaymentRepository payments, PaymentTransactionRepository transactions,
                          ChargingSessionRepository sessions, UserRepository users) {
        this.payments = payments;
        this.transactions = transactions;
        this.sessions = sessions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PaymentView getForSession(Long sessionId, Authentication auth) {
        ChargingSession session = ownedSession(sessionId, auth);
        return payments.findBySessionId(sessionId).map(PaymentView::from).orElseGet(() -> invoice(session));
    }

    @Transactional(readOnly = true)
    public List<PaymentView> getMine(Authentication auth) {
        User user = currentUser(auth);
        return payments.findForUser(user.getId()).stream().map(PaymentView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionView> getTransactionHistory(Authentication auth) {
        User user = currentUser(auth);
        return transactions.findForUser(user.getId()).stream().map(TransactionView::from).toList();
    }

    @Transactional
    public PaymentView attempt(Long sessionId, PaymentAttempt attempt, Authentication auth) {
        if (attempt == null || attempt.method() == null || !List.of("UPI", "CARD", "WALLET").contains(attempt.method())) {
            throw new IllegalArgumentException("Choose UPI, card, or wallet for this demo payment");
        }
        if (attempt.outcome() == null || !List.of("SUCCESS", "FAILURE").contains(attempt.outcome())) {
            throw new IllegalArgumentException("Choose a success or failure outcome for the demo");
        }
        ChargingSession session = ownedSession(sessionId, auth);
        if (!"COMPLETED".equalsIgnoreCase(session.getStatus())) throw new IllegalArgumentException("Payment is available after charging is completed");
        if (session.getEnergyUsed() == null || session.getEnergyUsed() <= 0) throw new IllegalArgumentException("Completed session has no billable energy recorded");

        Payment payment = payments.findBySessionId(sessionId).orElseGet(() -> {
            Payment p = new Payment(); p.setSession(session); p.setAmount(roundMoney(session.getEnergyUsed() * RATE_PER_KWH)); return p;
        });
        if ("PAID".equals(payment.getStatus())) throw new IllegalArgumentException("This session has already been paid");
        payment.setPaymentMethod(attempt.method());
        payment.setPaymentTime(LocalDateTime.now());
        if ("SUCCESS".equals(attempt.outcome())) {
            payment.setStatus("PAID");
            payment.setPaymentReference("DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else {
            payment.setStatus("FAILED");
            payment.setPaymentReference(null);
        }
        payment = payments.save(payment);
        transactions.save(new PaymentTransaction(session, payment.getAmount(), payment.getStatus(),
                payment.getPaymentMethod(), payment.getPaymentReference(), payment.getPaymentTime()));
        return PaymentView.from(payment);
    }

    private ChargingSession ownedSession(Long id, Authentication auth) {
        ChargingSession session = sessions.findById(id).orElseThrow(() -> new IllegalArgumentException("Charging session not found"));
        User current = currentUser(auth);
        if (!session.getRequest().getUser().getId().equals(current.getId())) throw new AccessDeniedException("This session does not belong to your account");
        return session;
    }
    private User currentUser(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow(() -> new AccessDeniedException("User account not found"));
    }
    private PaymentView invoice(ChargingSession s) {
        if (!"COMPLETED".equalsIgnoreCase(s.getStatus()) || s.getEnergyUsed() == null) throw new IllegalArgumentException("Invoice is available after charging is completed");
        return new PaymentView(null, s.getId(), roundMoney(s.getEnergyUsed() * RATE_PER_KWH), "UNPAID", null, null, null, s.getEnergyUsed(), RATE_PER_KWH);
    }
    private static double roundMoney(double amount) { return Math.round(amount * 100.0) / 100.0; }

    public record PaymentAttempt(String method, String outcome) {}
    public record TransactionView(Long id, Long sessionId, Double amount, String status, String paymentMethod,
                                  String paymentReference, LocalDateTime attemptedAt, Long requestId) {
        static TransactionView from(PaymentTransaction t) {
            return new TransactionView(t.getId(), t.getSession().getId(), t.getAmount(), t.getStatus(),
                    t.getPaymentMethod(), t.getPaymentReference(), t.getAttemptedAt(), null);
        }
    }
    public record PaymentView(Long id, Long sessionId, Double amount, String status, String paymentReference,
                              String paymentMethod, LocalDateTime paymentTime, Double energyKwh, Double ratePerKwh) {
        static PaymentView from(Payment p) {
            return new PaymentView(p.getId(), p.getSession().getId(), p.getAmount(), p.getStatus(), p.getPaymentReference(),
                    p.getPaymentMethod(), p.getPaymentTime(), p.getSession().getEnergyUsed(), RATE_PER_KWH);
        }
    }
}
