package ru.hotelbooking.hotel.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.hotel.HotelRepository;
import ru.hotelbooking.hotel.reservation.RoomReservationRepository;
import ru.hotelbooking.hotel.room.Room;
import ru.hotelbooking.hotel.room.RoomRepository;
import ru.hotelbooking.hotel.room.RoomType;

/**
 * Общий родитель интеграционных тестов. Профиль test выключает Eureka и демо-данные,
 * перед каждым тестом таблицы чистятся.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected HotelRepository hotelRepository;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected RoomReservationRepository reservationRepository;

    @BeforeEach
    void cleanDatabase() {
        reservationRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        hotelRepository.deleteAllInBatch();
    }

    protected static RequestPostProcessor admin() {
        return withRole("ADMIN");
    }

    protected static RequestPostProcessor user() {
        return withRole("USER");
    }

    protected static RequestPostProcessor internal() {
        return withRole("INTERNAL");
    }

    private static RequestPostProcessor withRole(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    protected Hotel givenHotel(String name, String city) {
        return hotelRepository.save(new Hotel(name, city, "Тестовая улица, 1", 4));
    }

    protected Room givenRoom(Hotel hotel, String number, long timesBooked) {
        return givenRoom(hotel, number, timesBooked, true);
    }

    protected Room givenRoom(Hotel hotel, String number, long timesBooked, boolean available) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setType(RoomType.STANDARD);
        room.setCapacity(2);
        room.setPricePerNight(new BigDecimal("4000.00"));
        room.setTimesBooked(timesBooked);
        room.setAvailable(available);
        return roomRepository.save(room);
    }

    protected String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}
