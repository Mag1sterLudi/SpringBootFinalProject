package ru.hotelbooking.hotel.room;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.reservation.RoomReservation;
import ru.hotelbooking.hotel.reservation.dto.AvailabilityRequest;
import ru.hotelbooking.hotel.room.dto.RoomRequest;
import ru.hotelbooking.hotel.support.IntegrationTest;

class RoomApiTest extends IntegrationTest {

    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(10);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);

    @Test
    @DisplayName("ADMIN добавляет номер; дубль номера в том же отеле - 409")
    void adminAddsRoomAndDuplicateIsRejected() throws Exception {
        Hotel hotel = givenHotel("Волга", "Казань");
        RoomRequest request = new RoomRequest(hotel.getId(), "101", RoomType.DELUXE, 2, new BigDecimal("5100"), null);

        mockMvc.perform(post("/api/rooms").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hotelId").value(hotel.getId()))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.timesBooked").value(0));

        mockMvc.perform(post("/api/rooms").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Рекомендации: только свободные номера, по возрастанию times_booked, при равенстве - по id")
    void recommendationsAreSortedByTimesBookedThenId() throws Exception {
        // given
        Hotel hotel = givenHotel("Волга", "Казань");
        Room popular = givenRoom(hotel, "1", 5);
        Room firstOfEqual = givenRoom(hotel, "2", 1);
        Room secondOfEqual = givenRoom(hotel, "3", 1);
        Room unused = givenRoom(hotel, "4", 0);
        givenRoom(hotel, "5", 0, false);
        Room busy = givenRoom(hotel, "6", 0);
        holdRoom(busy, CHECK_IN.plusDays(1), CHECK_OUT.plusDays(1));

        // when / then
        mockMvc.perform(get("/api/rooms/recommend").with(user())
                        .param("hotelId", hotel.getId().toString())
                        .param("startDate", CHECK_IN.toString())
                        .param("endDate", CHECK_OUT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value(unused.getId()))
                .andExpect(jsonPath("$[1].id").value(firstOfEqual.getId()))
                .andExpect(jsonPath("$[2].id").value(secondOfEqual.getId()))
                .andExpect(jsonPath("$[3].id").value(popular.getId()));
    }

    @Test
    @DisplayName("Свободные номера: занятый на пересекающиеся даты номер исключается, соседние даты - нет")
    void freeRoomsExcludeOverlappingHolds() throws Exception {
        Hotel hotel = givenHotel("Волга", "Казань");
        Room room = givenRoom(hotel, "1", 0);
        holdRoom(room, CHECK_IN, CHECK_OUT);

        mockMvc.perform(get("/api/rooms").with(user())
                        .param("startDate", CHECK_IN.plusDays(1).toString())
                        .param("endDate", CHECK_OUT.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // выезд одного гостя и заезд другого в один день не конфликтуют
        mockMvc.perform(get("/api/rooms").with(user())
                        .param("startDate", CHECK_OUT.toString())
                        .param("endDate", CHECK_OUT.plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Дата выезда раньше даты заезда - 400")
    void invalidPeriodIsRejected() throws Exception {
        mockMvc.perform(get("/api/rooms/recommend").with(user())
                        .param("startDate", CHECK_OUT.toString())
                        .param("endDate", CHECK_IN.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    private void holdRoom(Room room, LocalDate start, LocalDate end) {
        AvailabilityRequest slot = new AvailabilityRequest("hold-" + room.getId(), null, start, end);
        reservationRepository.save(RoomReservation.hold(room, slot, Instant.now()));
    }
}
