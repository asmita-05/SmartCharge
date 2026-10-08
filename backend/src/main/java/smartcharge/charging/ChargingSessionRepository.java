package smartcharge.charging;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    List<ChargingSession> findByRequestUserId(Long userId);

    List<ChargingSession> findByStatus(String status);

    List<ChargingSession> findByRequestIdOrderByStartTimeDesc(Long requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<ChargingSession> findFirstByChargerIdAndStatusOrderByStartTimeAsc(Long chargerId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from ChargingSession s where s.id = :id")
    java.util.Optional<ChargingSession> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
