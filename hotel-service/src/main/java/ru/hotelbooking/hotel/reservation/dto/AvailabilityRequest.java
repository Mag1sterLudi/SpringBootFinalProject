package ru.hotelbooking.hotel.reservation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import ru.hotelbooking.common.validation.ValidDateRange;
import ru.hotelbooking.hotel.reservation.SlotRequest;

@ValidDateRange
public record AvailabilityRequest(
        @NotBlank @Size(max = 64) String requestId,
        Long bookingId,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate endDate) implements SlotRequest {
}
