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

    @Column
    private Double estimatedWaitMinutes;

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

    @Column(name = "nlp_classification", length = 30)
    private String nlpClassification;

    @Column(name = "nlp_confidence")
    private Double nlpConfidence;

    @Column(name = "nlp_model", length = 160)
    private String nlpModel;

    @Column(name = "nlp_evaluated_at")
    private LocalDateTime nlpEvaluatedAt;

    @Column(name = "emergency_decision_source", length = 30)
    private String emergencyDecisionSource;

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

    public Double getEstimatedWaitMinutes() { return estimatedWaitMinutes; }

    public void setEstimatedWaitMinutes(Double estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }

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

    public String getNlpClassification() { return nlpClassification; }

    public void setNlpClassification(String nlpClassification) { this.nlpClassification = nlpClassification; }

    public Double getNlpConfidence() { return nlpConfidence; }

    public void setNlpConfidence(Double nlpConfidence) { this.nlpConfidence = nlpConfidence; }

    public String getNlpModel() { return nlpModel; }

    public void setNlpModel(String nlpModel) { this.nlpModel = nlpModel; }

    public LocalDateTime getNlpEvaluatedAt() { return nlpEvaluatedAt; }

    public void setNlpEvaluatedAt(LocalDateTime nlpEvaluatedAt) { this.nlpEvaluatedAt = nlpEvaluatedAt; }

    public String getEmergencyDecisionSource() { return emergencyDecisionSource; }

    public void setEmergencyDecisionSource(String emergencyDecisionSource) { this.emergencyDecisionSource = emergencyDecisionSource; }
}
