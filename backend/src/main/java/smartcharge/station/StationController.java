package smartcharge.station;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;
    private final StationRoutingService stationRoutingService;
    private final StationWaitService stationWaitService;

    public StationController(StationService stationService, StationRoutingService stationRoutingService,
            StationWaitService stationWaitService) {
        this.stationService = stationService;
        this.stationRoutingService = stationRoutingService;
        this.stationWaitService = stationWaitService;
    }

    @PostMapping
    public ResponseEntity<Station> createStation(@RequestBody Station station) {
        Station savedStation = stationService.createStation(station);
        return new ResponseEntity<>(savedStation, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Station>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/wait-estimates")
    public ResponseEntity<List<StationWaitEstimate>> getWaitEstimates() {
        return ResponseEntity.ok(stationWaitService.getWaitEstimates());
    }

    @GetMapping("/routes")
    public ResponseEntity<?> getStationRoutes(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        try {
            return ResponseEntity.ok(stationRoutingService.getRoutes(latitude, longitude));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                    "detail", exception.getReason() == null
                            ? "Route lookup failed. Check backend routing configuration." : exception.getReason()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Station> getStation(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Station> updateStation(
            @PathVariable Long id,
            @RequestBody Station station) {

        return ResponseEntity.ok(
                stationService.updateStation(id, station)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStation(@PathVariable Long id) {
        try {
            stationService.deleteStation(id);
            return ResponseEntity.ok("Station deleted successfully");
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This station cannot be deleted because it is still linked to chargers or charging records.");
        }
    }
}
