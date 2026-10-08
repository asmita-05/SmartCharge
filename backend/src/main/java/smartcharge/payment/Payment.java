package smartcharge.payment;

import jakarta.persistence.*;
import smartcharge.charging.ChargingSession;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private ChargingSession session;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String status;

    @Column
    private String paymentReference;

    @Column
    private String paymentMethod;

    @Column(nullable = false)
    private LocalDateTime paymentTime;

    public Payment() {
        this.status = "PENDING";
        this.paymentTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ChargingSession getSession() {
        return session;
    }

    public void setSession(ChargingSession session) {
        this.session = session;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }
}