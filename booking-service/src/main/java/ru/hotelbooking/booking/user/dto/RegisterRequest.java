package ru.hotelbooking.booking.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = UserValidation.USERNAME_REGEX) String username,
        @NotBlank @Size(min = 6, max = 100) String password) {
}
