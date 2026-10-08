package smartcharge.user;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin("*")
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
            @RequestBody Vehicle vehicle) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

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
            @PathVariable Long userId) {

        return vehicleRepository.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public Vehicle getVehicle(
            @PathVariable Long id) {

        return vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found"));
    }

    @PutMapping("/{id}")
    public Vehicle updateVehicle(
            @PathVariable Long id,
            @RequestBody Vehicle updatedVehicle) {

        Vehicle vehicle = getVehicle(id);

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
            @PathVariable Long id) {

        Vehicle vehicle = getVehicle(id);

        vehicleRepository.delete(vehicle);

        return "Vehicle deleted successfully";
    }
}