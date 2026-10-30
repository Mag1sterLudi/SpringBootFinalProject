package ru.hotelbooking.booking.user.dto;

import ru.hotelbooking.booking.user.Role;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, String username, Role role) {
}
