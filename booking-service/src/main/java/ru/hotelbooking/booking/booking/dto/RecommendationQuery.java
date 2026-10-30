package ru.hotelbooking.booking.booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import ru.hotelbooking.common.validation.DateRange;
import ru.hotelbooking.common.validation.ValidDateRange;

@ValidDateRange
public record RecommendationQuery(
        Long hotelId,
        @NotNull @FutureOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) implements DateRange {
}
