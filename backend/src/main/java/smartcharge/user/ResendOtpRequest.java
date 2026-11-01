package smartcharge.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendOtpRequest(@NotBlank @Email @Size(max = 254) String email) { }
