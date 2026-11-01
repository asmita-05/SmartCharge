package smartcharge.payment;

import jakarta.persistence.*;
import smartcharge.charging.ChargingSession;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_attempts")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChargingSession session;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String paymentMethod;

    @Column
    private String paymentReference;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    public PaymentTransaction() {}
    public PaymentTransaction(ChargingSession session, Double amount, String status, String paymentMethod,
                              String paymentReference, LocalDateTime attemptedAt) {
        this.session = session;
        this.amount = amount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.attemptedAt = attemptedAt;
    }
    public Long getId() { return id; }
    public ChargingSession getSession() { return session; }
    public Double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentReference() { return paymentReference; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
}
