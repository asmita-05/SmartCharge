package smartcharge.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import smartcharge.security.AuthTokenService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final EmailOtpService emailOtpService;
    private final AuthTokenService authTokenService;

    public UserController(UserService userService, EmailOtpService emailOtpService, AuthTokenService authTokenService) {
        this.userService = userService;
        this.emailOtpService = emailOtpService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            emailOtpService.startRegistration(request);
            return ResponseEntity.accepted().body(Map.of("message", "Verification code sent. Check your email."));
        } catch (UserService.DuplicateEmailException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email is already registered"));
        } catch (EmailOtpService.VerificationAlreadyPendingException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email verification is already pending. Request a new code.",
                            "verificationPending", true));
        } catch (UserService.InvalidPasswordException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must be at most 72 UTF-8 bytes"));
        } catch (BrevoEmailService.EmailConfigurationException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Email verification is not configured yet. Contact the administrator."));
        } catch (BrevoEmailService.EmailDeliveryException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "We could not send the verification email. Please try again later."));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyOtpRequest request) {
        EmailOtpService.VerificationResult result = emailOtpService.verify(request.email(), request.otp());
        return switch (result) {
            case VERIFIED -> ResponseEntity.ok(Map.of("message", "Email verified. Your account is ready."));
            case INVALID -> ResponseEntity.badRequest().body(Map.of("message", "That code is incorrect."));
            case EXPIRED -> ResponseEntity.status(HttpStatus.GONE).body(Map.of("message", "That code has expired. Request a new one."));
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No pending signup was found. Start again."));
            case TOO_MANY_ATTEMPTS -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", "Too many incorrect codes. Start signup again."));
            case ALREADY_REGISTERED -> ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email is already registered. Please sign in."));
        };
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<?> resend(@Valid @RequestBody ResendOtpRequest request) {
        try {
            emailOtpService.resend(request.email());
            return ResponseEntity.accepted().body(Map.of("message", "A new verification code has been sent."));
        } catch (EmailOtpService.VerificationNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No pending signup was found. Start again."));
        } catch (EmailOtpService.ResendCooldownException exception) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", "Please wait a minute before requesting another code."));
        } catch (EmailOtpService.ResendLimitException exception) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", "You have reached the resend limit. Try again after the code expires."));
        } catch (BrevoEmailService.EmailConfigurationException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Email verification is not configured yet. Contact the administrator."));
        } catch (BrevoEmailService.EmailDeliveryException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "We could not send the verification email. Please try again later."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.loginUser(request.email(), request.password());
            String accessToken = authTokenService.issue(user.getEmail(), user.getRole());
            return ResponseEntity.ok(AuthenticatedUserResponse.from(user, accessToken));
        } catch (UserService.InvalidCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        User user = userService.getUserById(id);
        requireOwner(user, authentication);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            org.springframework.security.core.Authentication authentication) {
        User user = userService.getUserById(id);
        requireOwner(user, authentication);
        try {
            return ResponseEntity.ok(UserResponse.from(userService.updateUser(id, request)));
        } catch (UserService.DuplicateEmailException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email is already registered"));
        } catch (UserService.InvalidPasswordException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must be at most 72 UTF-8 bytes"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        requireOwner(userService.getUserById(id), authentication);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authTokenService.revoke(authorization.substring(7).trim());
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private void requireOwner(User user, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !user.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only access your own account");
        }
    }
}
