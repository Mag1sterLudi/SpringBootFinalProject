package ru.hotelbooking.booking.client.dto;

import java.time.LocalDate;

public record HoldRequest(String requestId, Long bookingId, LocalDate startDate, LocalDate endDate) {
}
