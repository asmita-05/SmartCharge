package smartcharge.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 100) String name,
        @Email @Size(max = 254) String email,
        @Size(min = 8, max = 72) String password) { }
