package smartcharge.payment;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final BookingPaymentService bookingPaymentService;

    public PaymentController(PaymentService paymentService, BookingPaymentService bookingPaymentService) {
        this.paymentService = paymentService;
        this.bookingPaymentService = bookingPaymentService;
    }

    @GetMapping("/mine")
    public List<PaymentService.PaymentView> mine(Authentication authentication) {
        return paymentService.getMine(authentication);
    }

    @GetMapping("/attempts/mine")
    public List<PaymentService.TransactionView> attempts(Authentication authentication) {
        return java.util.stream.Stream.concat(paymentService.getTransactionHistory(authentication).stream(),
                        bookingPaymentService.history(authentication).stream())
                .sorted(Comparator.comparing(PaymentService.TransactionView::attemptedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @GetMapping("/booking/{requestId}")
    public BookingPaymentService.CheckoutView getBookingCheckout(@PathVariable Long requestId, Authentication authentication) {
        return bookingPaymentService.getCheckout(requestId, authentication);
    }

    @PostMapping("/booking/{requestId}/attempt")
    public BookingPaymentService.CheckoutView attemptBookingPayment(@PathVariable Long requestId,
            @RequestBody BookingPaymentService.PaymentAttempt attempt, Authentication authentication) {
        return bookingPaymentService.attempt(requestId, attempt, authentication);
    }

}
