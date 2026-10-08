package smartcharge.charging;

import jakarta.persistence.*;
import smartcharge.station.Charger;

import java.time.LocalDateTime;

@Entity
@Table(name = "charging_sessions")
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private ChargingRequest request;

    @ManyToOne
    @JoinColumn(name = "charger_id", nullable = false)
    private Charger charger;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false)
    private String status;

    @Column
    private Double energyUsed;

    @Column
    private Double actualMinutes;

    public ChargingSession() {
        this.status = "ACTIVE";
        this.startTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChargingRequest getRequest() {
        return request;
    }

    public void setRequest(ChargingRequest request) {
        this.request = request;
    }

    public Charger getCharger() {
        return charger;
    }

    public void setCharger(Charger charger) {
        this.charger = charger;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getEnergyUsed() {
        return energyUsed;
    }

    public void setEnergyUsed(Double energyUsed) {
        this.energyUsed = energyUsed;
    }

    public Double getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(Double actualMinutes) {
        this.actualMinutes = actualMinutes;
    }
}
