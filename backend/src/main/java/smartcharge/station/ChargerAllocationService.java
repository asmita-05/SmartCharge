package smartcharge.station;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartcharge.charging.ChargingRequest;
import smartcharge.charging.ChargingSession;

import java.util.Optional;

@Service
public class ChargerAllocationService {

    private final ChargerRepository chargerRepository;

    public ChargerAllocationService(ChargerRepository chargerRepository) {
        this.chargerRepository = chargerRepository;
    }

    @Transactional
    public Optional<ChargingSession> allocateCharger(
            ChargingRequest request) {

        Optional<Charger> chargerOptional =
                chargerRepository.findFirstByStatusOrderByIdAsc("AVAILABLE");

        if (chargerOptional.isEmpty()) {
            return Optional.empty();
        }

        Charger charger = chargerOptional.get();

        if (!isCompatible(request, charger)) {
            return Optional.empty();
        }

        charger.setStatus("OCCUPIED");
        chargerRepository.save(charger);

        ChargingSession session = new ChargingSession();
        session.setRequest(request);
        session.setCharger(charger);
        session.setStatus("ACTIVE");

        return Optional.of(session);
    }

    private boolean isCompatible(
            ChargingRequest request,
            Charger charger) {

        if (request.getRequiredConnectorType() == null
                || request.getRequiredConnectorType().isBlank()) {
            return true;
        }

        if (charger.getConnectorType() == null
                || charger.getConnectorType().isBlank()) {
            return false;
        }

        return request.getRequiredConnectorType()
                .equalsIgnoreCase(charger.getConnectorType());
    }
}