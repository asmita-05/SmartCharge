package smartcharge.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserId(Long userId);

    Optional<Vehicle> findByRegistrationNumber(
            String registrationNumber);
}