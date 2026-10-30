package ru.hotelbooking.booking.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import ru.hotelbooking.common.validation.DateRange;
import ru.hotelbooking.common.validation.ValidDateRange;

@ValidDateRange
public record CreateBookingRequest(
        @Schema(description = "Номер; обязателен при autoSelect=false, при autoSelect=true игнорируется")
        Long roomId,
        @Schema(description = "Ограничить автоподбор одним отелем")
        Long hotelId,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate endDate,
        boolean autoSelect,
        @Schema(description = "Ключ повторного запроса. Повторный запрос с тем же requestId вернёт ту же бронь")
        @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9._:-]+$") String requestId) implements DateRange {
}
