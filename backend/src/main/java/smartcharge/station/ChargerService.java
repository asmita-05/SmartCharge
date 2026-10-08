package smartcharge.station;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChargerService {

    private final ChargerRepository chargerRepository;
    private final StationRepository stationRepository;

    public ChargerService(
            ChargerRepository chargerRepository,
            StationRepository stationRepository) {

        this.chargerRepository = chargerRepository;
        this.stationRepository = stationRepository;
    }

    public Charger addCharger(
            Long stationId,
            Charger charger) {

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() ->
                        new RuntimeException("Station not found"));

        validateCharger(charger);

        charger.setStation(station);

        if (charger.getStatus() == null
                || charger.getStatus().isBlank()) {

            charger.setStatus("AVAILABLE");
        }

        validateStatus(charger.getStatus());

        return chargerRepository.save(charger);
    }

    public List<Charger> getAllChargers() {
        return chargerRepository.findAll();
    }

    public List<Charger> getChargersByStation(
            Long stationId) {

        return chargerRepository.findByStationId(stationId);
    }

    public Charger getChargerById(Long id) {

        return chargerRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Charger not found"));
    }

    public Charger updateCharger(
            Long id,
            Charger updatedCharger) {

        Charger charger = getChargerById(id);

        if (updatedCharger.getChargerType() != null) {
            charger.setChargerType(
                    updatedCharger.getChargerType());
        }

        if (updatedCharger.getConnectorType() != null) {
            charger.setConnectorType(
                    updatedCharger.getConnectorType());
        }

        if (updatedCharger.getPowerKw() != null) {

            if (updatedCharger.getPowerKw() <= 0) {
                throw new RuntimeException(
                        "Charger power must be greater than zero");
            }

            charger.setPowerKw(
                    updatedCharger.getPowerKw());
        }

        if (updatedCharger.getStatus() != null) {

            validateStatus(
                    updatedCharger.getStatus());

            charger.setStatus(
                    updatedCharger.getStatus().toUpperCase());
        }

        return chargerRepository.save(charger);
    }

    public Charger updateChargerStatus(
            Long id,
            String status) {

        Charger charger = getChargerById(id);

        validateStatus(status);

        charger.setStatus(
                status.toUpperCase());

        return chargerRepository.save(charger);
    }

    public void deleteCharger(Long id) {

        Charger charger = getChargerById(id);

        chargerRepository.delete(charger);
    }

    private void validateCharger(
            Charger charger) {

        if (charger.getPowerKw() == null
                || charger.getPowerKw() <= 0) {

            throw new RuntimeException(
                    "Charger power must be greater than zero");
        }

        if (charger.getConnectorType() == null
                || charger.getConnectorType().isBlank()) {

            throw new RuntimeException(
                    "Connector type is required");
        }
    }

    private void validateStatus(
            String status) {

        if (status == null || status.isBlank()) {
            throw new RuntimeException(
                    "Charger status is required");
        }

        String normalized =
                status.toUpperCase();

        if (!normalized.equals("AVAILABLE")
                && !normalized.equals("OCCUPIED")
                && !normalized.equals("UNAVAILABLE")
                && !normalized.equals("MAINTENANCE")) {

            throw new RuntimeException(
                    "Invalid charger status. Use AVAILABLE, OCCUPIED, UNAVAILABLE, or MAINTENANCE");
        }
    }
}