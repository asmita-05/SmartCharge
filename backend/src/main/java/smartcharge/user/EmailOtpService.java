package smartcharge.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class EmailOtpService {

    private static final int OTP_LIFETIME_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_RESENDS = 3;
    private static final int MAX_ATTEMPTS = 5;

    private final PendingRegistrationRepository pendingRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailOtpService(PendingRegistrationRepository pendingRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           BrevoEmailService emailService) {
        this.pendingRepository = pendingRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void startRegistration(RegisterRequest request) {
        String email = UserService.normalizeEmail(request.email());
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new UserService.DuplicateEmailException();
        }

        LocalDateTime now = LocalDateTime.now();
        PendingRegistration pending = pendingRepository.findByEmailForUpdate(email).orElse(null);
        if (pending != null && pending.getExpiresAt() != null && pending.getExpiresAt().isAfter(now)) {
            throw new VerificationAlreadyPendingException();
        }

        String code = newCode();
        if (pending == null) {
            pending = new PendingRegistration(email, request.name().trim(), passwordEncoder.encode(request.password()));
        } else {
            pending.setName(request.name().trim());
            pending.setPasswordHash(passwordEncoder.encode(request.password()));
            pending.setResendCount(0);
            pending.setFailedAttempts(0);
        }

        emailService.sendVerificationCode(email, code);
        updateCode(pending, code, now);
        pendingRepository.save(pending);
    }

    @Transactional
    public void resend(String rawEmail) {
        String email = UserService.normalizeEmail(rawEmail);
        PendingRegistration pending = pendingRepository.findByEmailForUpdate(email)
                .orElseThrow(VerificationNotFoundException::new);
        LocalDateTime now = LocalDateTime.now();
        boolean expired = pending.getExpiresAt() == null || !pending.getExpiresAt().isAfter(now);

        if (!expired && pending.getResendCount() >= MAX_RESENDS) {
            throw new ResendLimitException();
        }
        if (!expired && pending.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new ResendCooldownException();
        }

        String code = newCode();
        emailService.sendVerificationCode(email, code);
        if (expired) {
            pending.setResendCount(0);
            pending.setFailedAttempts(0);
        } else {
            pending.setResendCount(pending.getResendCount() + 1);
        }
        updateCode(pending, code, now);
        pendingRepository.save(pending);
    }

    @Transactional
    public VerificationResult verify(String rawEmail, String code) {
        String email = UserService.normalizeEmail(rawEmail);
        PendingRegistration pending = pendingRepository.findByEmailForUpdate(email).orElse(null);
        if (pending == null) return VerificationResult.NOT_FOUND;

        LocalDateTime now = LocalDateTime.now();
        if (pending.getExpiresAt() == null || !pending.getExpiresAt().isAfter(now)) {
            return VerificationResult.EXPIRED;
        }
        if (pending.getFailedAttempts() >= MAX_ATTEMPTS) {
            pendingRepository.delete(pending);
            return VerificationResult.TOO_MANY_ATTEMPTS;
        }
        if (pending.getOtpHash() == null || !passwordEncoder.matches(code, pending.getOtpHash())) {
            int attempts = pending.getFailedAttempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                pendingRepository.delete(pending);
                return VerificationResult.TOO_MANY_ATTEMPTS;
            }
            pending.setFailedAttempts(attempts);
            pendingRepository.save(pending);
            return VerificationResult.INVALID;
        }

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            pendingRepository.delete(pending);
            return VerificationResult.ALREADY_REGISTERED;
        }

        // A regular account is created only after the OTP matches and is unexpired.
        userRepository.save(new User(pending.getName(), pending.getEmail(), pending.getPasswordHash(), "USER"));
        pendingRepository.delete(pending);
        return VerificationResult.VERIFIED;
    }

    private void updateCode(PendingRegistration pending, String code, LocalDateTime now) {
        pending.setOtpHash(passwordEncoder.encode(code));
        pending.setExpiresAt(now.plusMinutes(OTP_LIFETIME_MINUTES));
        pending.setLastSentAt(now);
        pending.setFailedAttempts(0);
    }

    private String newCode() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    public enum VerificationResult { VERIFIED, INVALID, EXPIRED, NOT_FOUND, TOO_MANY_ATTEMPTS, ALREADY_REGISTERED }
    public static class VerificationAlreadyPendingException extends RuntimeException { }
    public static class VerificationNotFoundException extends RuntimeException { }
    public static class ResendLimitException extends RuntimeException { }
    public static class ResendCooldownException extends RuntimeException { }
}
