package ru.hotelbooking.hotel.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.reservation.dto.AvailabilityRequest;
import ru.hotelbooking.hotel.reservation.dto.ReleaseRequest;
import ru.hotelbooking.hotel.room.Room;
import ru.hotelbooking.hotel.support.IntegrationTest;

class AvailabilityApiTest extends IntegrationTest {

    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(7);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(2);

    private Room room;

    @BeforeEach
    void givenRoom() {
        Hotel hotel = givenHotel("Волга", "Казань");
        room = givenRoom(hotel, "101", 0);
    }

    @Test
    @DisplayName("Подтверждение доступности блокирует слот и увеличивает times_booked")
    void confirmHoldsSlot() throws Exception {
        confirm("req-1", 1L, CHECK_IN, CHECK_OUT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.roomId").value(room.getId()))
                .andExpect(jsonPath("$.bookingId").value(1));

        assertThat(timesBooked()).isEqualTo(1);
    }

    @Test
    @DisplayName("Повторная доставка того же requestId не создаёт второй слот и не меняет счётчик")
    void duplicateConfirmIsIdempotent() throws Exception {
        String first = confirm("req-1", 1L, CHECK_IN, CHECK_OUT).andReturn().getResponse().getContentAsString();
        String second = confirm("req-1", 1L, CHECK_IN, CHECK_OUT)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(timesBooked()).isEqualTo(1);
    }

    @Test
    @DisplayName("Пересекающийся период с другим requestId - 409 ROOM_NOT_AVAILABLE")
    void overlappingConfirmIsRejected() throws Exception {
        confirm("req-1", 1L, CHECK_IN, CHECK_OUT).andExpect(status().isOk());

        confirm("req-2", 2L, CHECK_IN.plusDays(1), CHECK_OUT.plusDays(1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_AVAILABLE"));

        assertThat(timesBooked()).isEqualTo(1);
    }

    @Test
    @DisplayName("Компенсация снимает блокировку, номер снова доступен; повторный release безопасен")
    void releaseFreesSlotAndIsIdempotent() throws Exception {
        confirm("req-1", 1L, CHECK_IN, CHECK_OUT).andExpect(status().isOk());

        release("req-1", 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("RELEASED"));
        release("req-1", 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALREADY_RELEASED"));

        assertThat(timesBooked()).isZero();
        confirm("req-2", 2L, CHECK_IN, CHECK_OUT).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Release без предварительной блокировки ничего не меняет")
    void releaseWithoutHoldIsNoop() throws Exception {
        release("req-1", 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NOTHING_TO_RELEASE"));

        assertThat(reservationRepository.count()).isZero();
        assertThat(timesBooked()).isZero();
    }

    @Test
    @DisplayName("Внутренние эндпойнты недоступны обычному пользователю - 403")
    void internalEndpointRequiresInternalRole() throws Exception {
        AvailabilityRequest request = new AvailabilityRequest("req-1", 1L, CHECK_IN, CHECK_OUT);

        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId()).with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isForbidden());
    }

    private ResultActions confirm(String requestId, Long bookingId, LocalDate start, LocalDate end)
            throws Exception {
        AvailabilityRequest request = new AvailabilityRequest(requestId, bookingId, start, end);
        return mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId()).with(internal())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));
    }

    private ResultActions release(String requestId, Long bookingId) throws Exception {
        ReleaseRequest request = new ReleaseRequest(requestId, bookingId, CHECK_IN, CHECK_OUT);
        return mockMvc.perform(post("/api/rooms/{id}/release", room.getId()).with(internal())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));
    }

    private long timesBooked() {
        return roomRepository.findById(room.getId()).orElseThrow().getTimesBooked();
    }
}
