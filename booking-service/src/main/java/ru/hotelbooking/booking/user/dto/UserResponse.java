package ru.hotelbooking.booking.user.dto;

import java.time.Instant;
import ru.hotelbooking.booking.user.Role;

public record UserResponse(Long id, String username, Role role, Instant createdAt) {
}
