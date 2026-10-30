package ru.hotelbooking.booking.booking.dto;

import java.time.Instant;
import java.time.LocalDate;
import ru.hotelbooking.booking.booking.BookingStatus;
import ru.hotelbooking.booking.booking.CancelReason;

public record BookingResponse(
        Long id,
        Long userId,
        Long roomId,
        Long hotelId,
        LocalDate startDate,
        LocalDate endDate,
        long nights,
        BookingStatus status,
        CancelReason cancelReason,
        String requestId,
        Instant createdAt,
        Instant updatedAt) {
}
