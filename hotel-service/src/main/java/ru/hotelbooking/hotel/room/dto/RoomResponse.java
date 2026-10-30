package ru.hotelbooking.hotel.room.dto;

import java.math.BigDecimal;
import ru.hotelbooking.hotel.room.RoomType;

public record RoomResponse(
        Long id,
        Long hotelId,
        String hotelName,
        String number,
        RoomType type,
        int capacity,
        BigDecimal pricePerNight,
        boolean available,
        long timesBooked) {
}
