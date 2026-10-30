package ru.hotelbooking.hotel.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 255) String address,
        @Min(1) @Max(5) int stars,
        @Size(max = 1000) String description) {
}
