package smartcharge.payment;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    public Payment createPayment(Payment payment) {

        if (payment.getAmount() == null || payment.getAmount() < 0) {
            throw new RuntimeException("Payment amount must be zero or greater");
        }

        if (payment.getStatus() == null || payment.getStatus().isBlank()) {
            payment.setStatus("PENDING");
        }

        return payment;
    }

    public Payment markAsPaid(Payment payment, String reference) {

        payment.setStatus("PAID");
        payment.setPaymentReference(reference);

        return payment;
    }

    public Payment markAsFailed(Payment payment) {

        payment.setStatus("FAILED");

        return payment;
    }

    public Payment refund(Payment payment) {

        payment.setStatus("REFUNDED");

        return payment;
    }
}