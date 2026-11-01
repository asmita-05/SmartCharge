package smartcharge.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String name, String email, String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new InvalidPasswordException();
        }
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException();
        }

        // Public registration can only create a driver account. Staff roles are
        // provisioned by an administrator and can never be selected by a client.
        return userRepository.save(new User(name.trim(), normalizedEmail,
                passwordEncoder.encode(password), "USER"));
    }

    @Transactional
    public User loginUser(String email, String password) {
        if (password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);
        String storedPassword = user.getPassword();
        boolean matched = storedPassword != null && password != null
                && (isBcryptHash(storedPassword)
                ? passwordEncoder.matches(password, storedPassword)
                : storedPassword.equals(password));

        if (!matched) {
            throw new InvalidCredentialsException();
        }

        // Upgrade legacy plaintext credentials on successful login.
        if (!isBcryptHash(storedPassword)) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }
        return user;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = getUserById(id);
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            String email = normalizeEmail(request.email());
            userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
                if (!existing.getId().equals(id)) throw new DuplicateEmailException();
            });
            user.setEmail(email);
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new InvalidPasswordException();
            }
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(getUserById(id));
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    public static class DuplicateEmailException extends RuntimeException { }
    public static class InvalidPasswordException extends RuntimeException { }
    public static class InvalidCredentialsException extends RuntimeException { }
}
