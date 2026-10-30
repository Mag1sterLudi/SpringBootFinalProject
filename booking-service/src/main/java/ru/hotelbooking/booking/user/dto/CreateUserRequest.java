package ru.hotelbooking.booking.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.hotelbooking.booking.user.Role;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = UserValidation.USERNAME_REGEX) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotNull Role role) {
}
