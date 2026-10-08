package smartcharge.user;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String vehicleType;

    @Column(nullable = false)
    private Double batteryCapacity;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Vehicle() {
    }

    public Vehicle(String registrationNumber, String model,
                   String vehicleType, Double batteryCapacity, User user) {
        this.registrationNumber = registrationNumber;
        this.model = model;
        this.vehicleType = vehicleType;
        this.batteryCapacity = batteryCapacity;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Double getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(Double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}