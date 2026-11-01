package smartcharge.user;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PendingRegistrationCleanup {

    private final PendingRegistrationRepository repository;

    public PendingRegistrationCleanup(PendingRegistrationRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelay = 86_400_000L)
    @Transactional
    public void removeExpiredSignups() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
