package ru.hotelbooking.booking.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.hotelbooking.booking.user.Role;

/**
 * Частичное обновление: null-поля не меняются.
 */
public record UpdateUserRequest(
        @Size(min = 3, max = 50) @Pattern(regexp = UserValidation.USERNAME_REGEX) String username,
        @Size(min = 6, max = 100) String password,
        Role role) {
}
