package smartcharge.station;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public Station createStation(Station station) {
        if (station.getStatus() == null || station.getStatus().isBlank()) {
            station.setStatus("ACTIVE");
        }

        return stationRepository.save(station);
    }

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found"));
    }

    public Station updateStation(Long id, Station updatedStation) {
        Station existingStation = getStationById(id);

        if (updatedStation.getName() != null
                && !updatedStation.getName().isBlank()) {
            existingStation.setName(updatedStation.getName());
        }

        if (updatedStation.getLocation() != null
                && !updatedStation.getLocation().isBlank()) {
            existingStation.setLocation(updatedStation.getLocation());
        }

        if (updatedStation.getStatus() != null
                && !updatedStation.getStatus().isBlank()) {
            existingStation.setStatus(updatedStation.getStatus());
        }

        return stationRepository.save(existingStation);
    }

    public void deleteStation(Long id) {
        Station station = getStationById(id);
        stationRepository.delete(station);
    }
}