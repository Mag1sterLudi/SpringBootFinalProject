package ru.hotelbooking.hotel.room.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import ru.hotelbooking.hotel.room.RoomType;

/**
 * Фильтры поиска. Без дат возвращаются все номера в эксплуатации.
 */
public record RoomSearchCriteria(
        Long hotelId,
        String city,
        RoomType type,
        @Min(1) Integer minCapacity,
        @DecimalMin("0.0") BigDecimal maxPrice,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    public boolean hasPeriod() {
        return startDate != null && endDate != null;
    }
}
