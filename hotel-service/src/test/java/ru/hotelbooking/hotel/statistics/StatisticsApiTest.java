package ru.hotelbooking.hotel.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.reservation.RoomReservation;
import ru.hotelbooking.hotel.reservation.dto.AvailabilityRequest;
import ru.hotelbooking.hotel.room.Room;
import ru.hotelbooking.hotel.support.IntegrationTest;

class StatisticsApiTest extends IntegrationTest {

    private static final LocalDate FROM = LocalDate.now().plusDays(1);
    private static final LocalDate TO = FROM.plusDays(10);

    @Test
    @DisplayName("ADMIN получает загруженность отеля с разбивкой по номерам")
    void adminSeesHotelOccupancy() throws Exception {
        Hotel hotel = givenHotel("Волга", "Казань");
        Room room = givenRoom(hotel, "1", 1);
        givenRoom(hotel, "2", 0);
        AvailabilityRequest slot = new AvailabilityRequest("stat-1", 1L, FROM, FROM.plusDays(5));
        reservationRepository.save(RoomReservation.hold(room, slot, Instant.now()));

        mockMvc.perform(get("/api/hotels/{id}/statistics", hotel.getId()).with(admin())
                        .param("from", FROM.toString())
                        .param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedNights").value(5))
                .andExpect(jsonPath("$.availableNights").value(20))
                .andExpect(jsonPath("$.occupancyRate").value(0.25))
                .andExpect(jsonPath("$.rooms.length()").value(2));
    }

    @Test
    @DisplayName("Сводка по всем отелям доступна только ADMIN")
    void summaryIsAdminOnly() throws Exception {
        givenHotel("Волга", "Казань");

        mockMvc.perform(get("/api/hotels/statistics").with(user()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hotels/statistics").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rooms").doesNotExist());
    }
}
