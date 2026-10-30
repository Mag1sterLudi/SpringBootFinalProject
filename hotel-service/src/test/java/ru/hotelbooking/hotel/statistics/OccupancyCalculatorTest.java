package ru.hotelbooking.hotel.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.reservation.RoomReservation;
import ru.hotelbooking.hotel.reservation.dto.AvailabilityRequest;
import ru.hotelbooking.hotel.room.Room;
import ru.hotelbooking.hotel.statistics.dto.HotelOccupancy;

class OccupancyCalculatorTest {

    private static final LocalDate FROM = LocalDate.of(2030, 1, 1);
    private static final LocalDate TO = FROM.plusDays(10);

    private final OccupancyCalculator calculator = new OccupancyCalculator();

    @Test
    @DisplayName("Загрузка считается по номеро-ночам только для номеров в эксплуатации")
    void calculatesOccupancyForRoomsInService() {
        // given
        Hotel hotel = new Hotel("Волга", "Казань", "ул. Баумана, 1", 3);
        hotel.setId(1L);
        Room busy = room(hotel, 1L, 4, true);
        Room idle = room(hotel, 2L, 0, true);
        Room underRepair = room(hotel, 3L, 9, false);
        List<RoomReservation> reservations = List.of(
                reservation(busy, FROM.minusDays(2), FROM.plusDays(3)),
                reservation(busy, FROM.plusDays(8), TO.plusDays(5)));

        // when
        HotelOccupancy result = calculator.calculate(hotel, List.of(busy, idle, underRepair), reservations, FROM, TO);

        // then
        assertThat(result.totalRooms()).isEqualTo(3);
        assertThat(result.roomsInService()).isEqualTo(2);
        assertThat(result.bookedNights()).isEqualTo(5);
        assertThat(result.availableNights()).isEqualTo(20);
        assertThat(result.occupancyRate()).isEqualTo(0.25);
        assertThat(result.idleRoomIds()).containsExactly(2L);
        assertThat(result.averageTimesBooked()).isEqualTo(2.0);
        assertThat(result.rooms()).hasSize(3);
    }

    @Test
    @DisplayName("Отель без номеров даёт нулевую статистику без деления на ноль")
    void emptyHotelHasZeroOccupancy() {
        Hotel hotel = new Hotel("Пусто", "Тверь", "ул. Советская, 1", 2);

        HotelOccupancy result = calculator.calculate(hotel, List.of(), List.of(), FROM, TO);

        assertThat(result.occupancyRate()).isZero();
        assertThat(result.averageTimesBooked()).isZero();
        assertThat(result.idleRoomIds()).isEmpty();
    }

    private static Room room(Hotel hotel, Long id, long timesBooked, boolean available) {
        Room room = new Room();
        room.setId(id);
        room.setHotel(hotel);
        room.setNumber(String.valueOf(100 + id));
        room.setPricePerNight(BigDecimal.TEN);
        room.setTimesBooked(timesBooked);
        room.setAvailable(available);
        return room;
    }

    private static RoomReservation reservation(Room room, LocalDate start, LocalDate end) {
        AvailabilityRequest slot = new AvailabilityRequest("r-" + start, null, start, end);
        return RoomReservation.hold(room, slot, Instant.now());
    }
}
