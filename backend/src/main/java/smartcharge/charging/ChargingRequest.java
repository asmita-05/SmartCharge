package smartcharge.charging;

import jakarta.persistence.*;
import smartcharge.user.User;
import smartcharge.user.Vehicle;
import smartcharge.station.Station;
import smartcharge.station.Charger;

import java.time.LocalDateTime;

@Entity
@Table(name = "charging_requests")
public class ChargingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Station station;

    @ManyToOne
    @JoinColumn(name = "requested_charger_id")
    private Charger requestedCharger;

    @Column(nullable = false)
    private Double currentBattery;

    @Column(nullable = false)
    private Double targetBattery;

    @Column 
    private Double estimatedMinutes;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private Boolean emergencyRequested;

    @Column(nullable = false)
    private String emergencyStatus;

    @Column 
    private String emergencyReason;

    @Column
    private String requiredConnectorType;

    @Column
    private Boolean adminApproved = false;

    @Column
    private String adminDecision;

    public ChargingRequest() {
        this.arrivalTime = LocalDateTime.now();
        this.status = "PENDING";
        this.priority = "NORMAL";
        this.emergencyRequested = false;
        this.emergencyStatus = "NONE";
        this.adminApproved = false;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Station getStation() { return station; }

    public void setStation(Station station) { this.station = station; }

    public Charger getRequestedCharger() { return requestedCharger; }

    public void setRequestedCharger(Charger requestedCharger) { this.requestedCharger = requestedCharger; }

    public Double getCurrentBattery() {
        return currentBattery;
    }

    public void setCurrentBattery(Double currentBattery) {
        this.currentBattery = currentBattery;
    }

    public Double getTargetBattery() {
        return targetBattery;
    }

    public void setTargetBattery(Double targetBattery) {
        this.targetBattery = targetBattery;
    }

    public Double getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Double estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Boolean getEmergencyRequested() {
        return emergencyRequested;
    }

    public void setEmergencyRequested(Boolean emergencyRequested) {
        this.emergencyRequested = emergencyRequested;
    }

    public String getEmergencyStatus() {
        return emergencyStatus;
    }

    public void setEmergencyStatus(String emergencyStatus) {
        this.emergencyStatus = emergencyStatus;
    }

    public String getEmergencyReason() {
        return emergencyReason;
    }

    public void setEmergencyReason(String emergencyReason) {
        this.emergencyReason = emergencyReason;
    }

    public String getRequiredConnectorType() {
        return requiredConnectorType;
    }

    public void setRequiredConnectorType(String requiredConnectorType) {
        this.requiredConnectorType = requiredConnectorType;
    }

    public Boolean getAdminApproved() { return adminApproved; }

    public void setAdminApproved(Boolean adminApproved) { this.adminApproved = adminApproved; }

    public String getAdminDecision() { return adminDecision; }

    public void setAdminDecision(String adminDecision) { this.adminDecision = adminDecision; }
}
