package ru.hotelbooking.hotel.hotel.dto;

import java.time.Instant;

public record HotelResponse(
        Long id,
        String name,
        String city,
        String address,
        int stars,
        String description,
        Instant createdAt) {
}
