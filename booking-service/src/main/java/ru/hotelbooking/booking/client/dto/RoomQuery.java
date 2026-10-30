package ru.hotelbooking.booking.client.dto;

import java.time.LocalDate;

public record RoomQuery(Long hotelId, LocalDate startDate, LocalDate endDate, int limit) {
}
