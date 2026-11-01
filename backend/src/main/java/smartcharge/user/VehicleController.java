package smartcharge.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleController(
            VehicleRepository vehicleRepository,
            UserRepository userRepository) {

        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/user/{userId}")
    public Vehicle addVehicle(
            @PathVariable Long userId,
            @RequestBody Vehicle vehicle,
            Authentication authentication) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
        requireOwner(user, authentication);

        vehicle.setUser(user);

        if (vehicle.getBatteryCapacity() == null
                || vehicle.getBatteryCapacity() <= 0) {

            throw new RuntimeException(
                    "Battery capacity must be greater than zero");
        }

        return vehicleRepository.save(vehicle);
    }

    @GetMapping("/user/{userId}")
    public List<Vehicle> getUserVehicles(
            @PathVariable Long userId,
            Authentication authentication) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        requireOwner(user, authentication);

        return vehicleRepository.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public Vehicle getVehicle(
            @PathVariable Long id,
            Authentication authentication) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found"));
        requireOwner(vehicle.getUser(), authentication);
        return vehicle;
    }

    @PutMapping("/{id}")
    public Vehicle updateVehicle(
            @PathVariable Long id,
            @RequestBody Vehicle updatedVehicle,
            Authentication authentication) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        requireOwner(vehicle.getUser(), authentication);

        vehicle.setRegistrationNumber(
                updatedVehicle.getRegistrationNumber());

        vehicle.setModel(
                updatedVehicle.getModel());

        vehicle.setVehicleType(
                updatedVehicle.getVehicleType());

        vehicle.setBatteryCapacity(
                updatedVehicle.getBatteryCapacity());

        if (vehicle.getBatteryCapacity() == null
                || vehicle.getBatteryCapacity() <= 0) {

            throw new RuntimeException(
                    "Battery capacity must be greater than zero");
        }

        return vehicleRepository.save(vehicle);
    }

    @DeleteMapping("/{id}")
    public String deleteVehicle(
            @PathVariable Long id,
            Authentication authentication) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        requireOwner(vehicle.getUser(), authentication);

        vehicleRepository.delete(vehicle);

        return "Vehicle deleted successfully";
    }

    private void requireOwner(User user, Authentication authentication) {
        if (authentication == null || !user.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new AccessDeniedException("You can only access your own vehicles");
        }
    }
}
