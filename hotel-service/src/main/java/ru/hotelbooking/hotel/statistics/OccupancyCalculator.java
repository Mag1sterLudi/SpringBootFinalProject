package ru.hotelbooking.hotel.statistics;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.hotelbooking.common.validation.DateRanges;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.reservation.RoomReservation;
import ru.hotelbooking.hotel.room.Room;
import ru.hotelbooking.hotel.statistics.dto.HotelOccupancy;
import ru.hotelbooking.hotel.statistics.dto.RoomOccupancy;

/**
 * Расчёт загруженности. В БД не ходит, поэтому тестируется обычным unit-тестом.
 */
@Component
public class OccupancyCalculator {

    public HotelOccupancy calculate(Hotel hotel, List<Room> rooms, List<RoomReservation> reservations,
                                    LocalDate from, LocalDate to) {
        long periodNights = DateRanges.nights(from, to);
        Map<Long, List<RoomReservation>> byRoom = reservations.stream()
                .collect(Collectors.groupingBy(reservation -> reservation.getRoom().getId()));

        List<RoomOccupancy> roomStats = rooms.stream()
                .map(room -> roomOccupancy(room, byRoom.getOrDefault(room.getId(), List.of()), from, to, periodNights))
                .toList();
        List<RoomOccupancy> inService = roomStats.stream().filter(RoomOccupancy::available).toList();

        long bookedNights = inService.stream().mapToLong(RoomOccupancy::bookedNights).sum();
        long availableNights = periodNights * inService.size();
        List<Long> idleRooms = inService.stream()
                .filter(room -> room.bookedNights() == 0)
                .map(RoomOccupancy::roomId)
                .toList();

        return new HotelOccupancy(
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                from,
                to,
                rooms.size(),
                inService.size(),
                bookedNights,
                availableNights,
                ratio(bookedNights, availableNights),
                round(average(inService)),
                idleRooms,
                roomStats);
    }

    private RoomOccupancy roomOccupancy(Room room, Collection<RoomReservation> reservations,
                                        LocalDate from, LocalDate to, long periodNights) {
        long nights = reservations.stream()
                .mapToLong(r -> DateRanges.overlapNights(r.getStartDate(), r.getEndDate(), from, to))
                .sum();
        return new RoomOccupancy(
                room.getId(),
                room.getNumber(),
                room.getType(),
                room.isAvailable(),
                room.getTimesBooked(),
                reservations.size(),
                nights,
                ratio(nights, periodNights));
    }

    private static double average(List<RoomOccupancy> rooms) {
        return rooms.stream().mapToLong(RoomOccupancy::timesBooked).average().orElse(0);
    }

    private static double ratio(long part, long total) {
        return total == 0 ? 0 : round((double) part / total);
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
