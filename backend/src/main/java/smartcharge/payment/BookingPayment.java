package smartcharge.payment;

import jakarta.persistence.*;
import smartcharge.charging.ChargingRequest;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_payments")
public class BookingPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private ChargingRequest request;
    @Column(nullable = false) private Double amount;
    @Column(nullable = false) private String status;
    @Column private String paymentReference;
    @Column private String paymentMethod;
    @Column(nullable = false) private LocalDateTime paymentTime;
    public BookingPayment() {}
    public BookingPayment(ChargingRequest request, Double amount) {
        this.request = request; this.amount = amount; this.status = "PENDING"; this.paymentTime = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public ChargingRequest getRequest() { return request; }
    public Double getAmount() { return amount; }
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
}
