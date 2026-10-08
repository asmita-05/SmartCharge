package smartcharge.station;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chargers")
@CrossOrigin(origins = "*")
public class ChargerController {

    private final ChargerService chargerService;

    public ChargerController(ChargerService chargerService) {
        this.chargerService = chargerService;
    }

    @PostMapping("/station/{stationId}")
    public ResponseEntity<Charger> addCharger(
            @PathVariable Long stationId,
            @RequestBody Charger charger) {

        Charger savedCharger =
                chargerService.addCharger(stationId, charger);

        return new ResponseEntity<>(savedCharger, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Charger>> getAllChargers() {
        return ResponseEntity.ok(
                chargerService.getAllChargers()
        );
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<List<Charger>> getChargersByStation(
            @PathVariable Long stationId) {

        return ResponseEntity.ok(
                chargerService.getChargersByStation(stationId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Charger> getCharger(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                chargerService.getChargerById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Charger> updateCharger(
            @PathVariable Long id,
            @RequestBody Charger charger) {

        return ResponseEntity.ok(
                chargerService.updateCharger(id, charger)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Charger> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        return ResponseEntity.ok(
                chargerService.updateChargerStatus(id, status)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCharger(
            @PathVariable Long id) {

        chargerService.deleteCharger(id);

        return ResponseEntity.ok("Charger deleted successfully");
    }
}