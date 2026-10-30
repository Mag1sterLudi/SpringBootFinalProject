package ru.hotelbooking.booking.client.dto;

import java.math.BigDecimal;

public record RoomView(
        Long id,
        Long hotelId,
        String hotelName,
        String number,
        String type,
        int capacity,
        BigDecimal pricePerNight,
        boolean available,
        long timesBooked) {
}
