package ru.hotelbooking.hotel.statistics.dto;

import ru.hotelbooking.hotel.room.RoomType;

public record RoomOccupancy(
        Long roomId,
        String number,
        RoomType type,
        boolean available,
        long timesBooked,
        int reservations,
        long bookedNights,
        double occupancyRate) {
}
