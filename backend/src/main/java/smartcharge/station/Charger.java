package smartcharge.station;

import jakarta.persistence.*;

@Entity
@Table(name = "chargers")
public class Charger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String chargerType;

    @Column(nullable = false)
    private String connectorType;

    @Column(nullable = false)
    private Double powerKw;

    @Column(nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    public Charger() {
    }

    public Charger(String chargerType, String connectorType,
                    Double powerKw, String status, Station station) {
        this.chargerType = chargerType;
        this.connectorType = connectorType;
        this.powerKw = powerKw;
        this.status = status;
        this.station = station;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChargerType() {
        return chargerType;
    }

    public void setChargerType(String chargerType) {
        this.chargerType = chargerType;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public Double getPowerKw() {
        return powerKw;
    }

    public void setPowerKw(Double powerKw) {
        this.powerKw = powerKw;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }
}