package ru.hotelbooking.booking.client.dto;

import java.time.LocalDate;

public record ReleaseRequest(String requestId, Long bookingId, LocalDate startDate, LocalDate endDate) {
}
