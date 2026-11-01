package smartcharge.user;

import java.time.LocalDateTime;

public record UserResponse(Long id, String name, String email, String role, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
