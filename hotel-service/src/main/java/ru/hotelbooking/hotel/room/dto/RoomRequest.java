package ru.hotelbooking.hotel.room.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ru.hotelbooking.hotel.room.RoomType;

public record RoomRequest(
        @NotNull Long hotelId,
        @NotBlank @Size(max = 10) String number,
        @NotNull RoomType type,
        @Min(1) @Max(10) int capacity,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 8, fraction = 2)
        BigDecimal pricePerNight,
        Boolean available) {
}
