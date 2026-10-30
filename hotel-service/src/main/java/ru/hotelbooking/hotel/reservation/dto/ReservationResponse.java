package ru.hotelbooking.hotel.reservation.dto;

import java.time.LocalDate;
import ru.hotelbooking.hotel.reservation.ReservationStatus;

public record ReservationResponse(
        Long reservationId,
        Long roomId,
        Long hotelId,
        String requestId,
        Long bookingId,
        LocalDate startDate,
        LocalDate endDate,
        ReservationStatus status) {
}
