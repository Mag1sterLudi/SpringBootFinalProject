package ru.hotelbooking.hotel.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.hotel.HotelRepository;
import ru.hotelbooking.hotel.reservation.RoomReservation;
import ru.hotelbooking.hotel.reservation.RoomReservationRepository;
import ru.hotelbooking.hotel.reservation.SlotRequest;
import ru.hotelbooking.hotel.room.Room;
import ru.hotelbooking.hotel.room.RoomRepository;
import ru.hotelbooking.hotel.room.RoomType;

/**
 * Демо-данные (описаны в README, раздел "Демо-данные").
 * У номеров разный times_booked, чтобы была видна сортировка,
 * 105 на ремонте, 101 занят бронью Ивана - на эти даты можно получить 409.
 * id номеров идут по порядку вставки (1..10), booking-service на них ссылается.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements ApplicationRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (hotelRepository.count() > 0) {
            return;
        }
        Hotel spb = hotelRepository.save(hotel("Северная звезда", "Санкт-Петербург", "Невский проспект, 48", 4));
        Hotel kazan = hotelRepository.save(hotel("Волжский берег", "Казань", "ул. Баумана, 12", 3));
        Hotel sochi = hotelRepository.save(hotel("Горный воздух", "Сочи", "Курортный проспект, 75", 5));

        Room room101 = room(spb, "101", RoomType.STANDARD, 2, "4500", 5, true);
        Room room102 = room(spb, "102", RoomType.STANDARD, 2, "4500", 2, true);
        room(spb, "103", RoomType.DELUXE, 3, "7200", 0, true);
        room(spb, "104", RoomType.SUITE, 4, "12000", 1, true);
        room(spb, "105", RoomType.STANDARD, 2, "4500", 7, false);
        Room room201 = room(kazan, "201", RoomType.STANDARD, 2, "3200", 3, true);
        room(kazan, "202", RoomType.STANDARD, 2, "3200", 3, true);
        room(kazan, "203", RoomType.DELUXE, 2, "5100", 0, true);
        Room room301 = room(sochi, "301", RoomType.DELUXE, 2, "9800", 4, true);
        room(sochi, "302", RoomType.SUITE, 4, "15500", 1, true);

        LocalDate today = LocalDate.now();
        hold(room101, "seed-booking-1", 1L, today.plusDays(5), today.plusDays(8));
        hold(room201, "seed-booking-2", 2L, today.plusDays(10), today.plusDays(12));
        hold(room301, "seed-booking-3", 3L, today.plusDays(3), today.plusDays(7));
        hold(room102, "seed-booking-4", 4L, today.minusDays(20), today.minusDays(17));

        log.info("Demo data loaded: {} hotels, {} rooms, {} reservations",
                hotelRepository.count(), roomRepository.count(), reservationRepository.count());
    }

    private static Hotel hotel(String name, String city, String address, int stars) {
        Hotel hotel = new Hotel(name, city, address, stars);
        hotel.setDescription("Демонстрационный отель");
        return hotel;
    }

    private Room room(Hotel hotel, String number, RoomType type, int capacity, String price,
                      long timesBooked, boolean available) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setType(type);
        room.setCapacity(capacity);
        room.setPricePerNight(new BigDecimal(price));
        room.setTimesBooked(timesBooked);
        room.setAvailable(available);
        return roomRepository.save(room);
    }

    private void hold(Room room, String requestId, Long bookingId, LocalDate start, LocalDate end) {
        Instant now = Instant.now();
        reservationRepository.save(RoomReservation.hold(room, new SeedSlot(requestId, bookingId, start, end), now));
    }

    private record SeedSlot(String requestId, Long bookingId, LocalDate startDate, LocalDate endDate)
            implements SlotRequest {
    }
}
