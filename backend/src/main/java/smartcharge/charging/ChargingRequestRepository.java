package smartcharge.charging;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChargingRequestRepository extends JpaRepository<ChargingRequest, Long> {

    List<ChargingRequest> findByUserId(Long userId);

    List<ChargingRequest> findByStatus(String status);

    List<ChargingRequest> findByStatusAndAdminApprovedTrue(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ChargingRequest r where r.id = :id")
    java.util.Optional<ChargingRequest> findByIdForUpdate(@Param("id") Long id);
}
