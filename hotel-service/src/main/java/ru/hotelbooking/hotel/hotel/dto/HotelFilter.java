package ru.hotelbooking.hotel.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record HotelFilter(
        String city,
        String name,
        @Min(1) @Max(5) Integer minStars) {
}
