package smartcharge.payment;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin("*")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public PaymentController(
            PaymentRepository paymentRepository,
            PaymentService paymentService) {

        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    @PostMapping
    public Payment createPayment(@RequestBody Payment payment) {
        Payment createdPayment = paymentService.createPayment(payment);
        return paymentRepository.save(createdPayment);
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Payment getPayment(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    @GetMapping("/session/{sessionId}")
    public Payment getPaymentBySession(@PathVariable Long sessionId) {
        return paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    @GetMapping("/status/{status}")
    public List<Payment> getPaymentsByStatus(
            @PathVariable String status) {

        return paymentRepository.findByStatus(status);
    }

    @PutMapping("/{id}/paid")
    public Payment markAsPaid(
            @PathVariable Long id,
            @RequestParam String reference) {

        Payment payment = getPayment(id);

        Payment updatedPayment =
                paymentService.markAsPaid(payment, reference);

        return paymentRepository.save(updatedPayment);
    }

    @PutMapping("/{id}/failed")
    public Payment markAsFailed(@PathVariable Long id) {

        Payment payment = getPayment(id);

        Payment updatedPayment =
                paymentService.markAsFailed(payment);

        return paymentRepository.save(updatedPayment);
    }

    @PutMapping("/{id}/refund")
    public Payment refund(@PathVariable Long id) {

        Payment payment = getPayment(id);

        Payment updatedPayment =
                paymentService.refund(payment);

        return paymentRepository.save(updatedPayment);
    }
}