package smartcharge.payment;

import jakarta.persistence.*;
import smartcharge.charging.ChargingRequest;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_payment_attempts")
public class BookingPaymentAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ChargingRequest request;
    @Column(nullable = false) private Double amount;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private String paymentMethod;
    @Column private String paymentReference;
    @Column(nullable = false) private LocalDateTime attemptedAt;
    public BookingPaymentAttempt() {}
    public BookingPaymentAttempt(ChargingRequest request, Double amount, String status, String method,
                                 String reference, LocalDateTime attemptedAt) {
        this.request = request; this.amount = amount; this.status = status; this.paymentMethod = method;
        this.paymentReference = reference; this.attemptedAt = attemptedAt;
    }
    public Long getId() { return id; }
    public ChargingRequest getRequest() { return request; }
    public Double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentReference() { return paymentReference; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
}
