package smartcharge.user;

import java.time.LocalDateTime;

public record AuthenticatedUserResponse(Long id, String name, String email, String role,
                                        LocalDateTime createdAt, String accessToken) {
    public static AuthenticatedUserResponse from(User user, String accessToken) {
        return new AuthenticatedUserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.getCreatedAt(), accessToken);
    }
}
