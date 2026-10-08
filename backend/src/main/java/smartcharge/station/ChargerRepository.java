package smartcharge.station;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChargerRepository extends JpaRepository<Charger, Long> {

    List<Charger> findByStationId(Long stationId);

    List<Charger> findByStatus(String status);

    List<Charger> findByStatusAndConnectorType(String status, String connectorType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Charger> findFirstByStatusOrderByIdAsc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Charger> findFirstByStatusAndConnectorTypeOrderByIdAsc(
            String status,
            String connectorType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Charger c where c.id = :id")
    Optional<Charger> findByIdForUpdate(@Param("id") Long id);
}
