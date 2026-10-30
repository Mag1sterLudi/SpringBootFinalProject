package ru.hotelbooking.hotel.statistics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

/**
 * Загруженность отеля за период с from по to. occupancyRate - доля занятых номеро-ночей (только номера
 * в эксплуатации), idleRoomIds - номера, которые весь период простояли.
 */
public record HotelOccupancy(
        Long hotelId,
        String hotelName,
        String city,
        LocalDate from,
        LocalDate to,
        int totalRooms,
        int roomsInService,
        long bookedNights,
        long availableNights,
        double occupancyRate,
        double averageTimesBooked,
        List<Long> idleRoomIds,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<RoomOccupancy> rooms) {

    public HotelOccupancy withoutRooms() {
        return new HotelOccupancy(hotelId, hotelName, city, from, to, totalRooms, roomsInService, bookedNights,
                availableNights, occupancyRate, averageTimesBooked, idleRoomIds, List.of());
    }
}
