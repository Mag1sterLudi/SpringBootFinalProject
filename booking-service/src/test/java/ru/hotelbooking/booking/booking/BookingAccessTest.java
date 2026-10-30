package ru.hotelbooking.booking.booking;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.hotelbooking.booking.booking.dto.CreateBookingRequest;
import ru.hotelbooking.booking.support.BookingIntegrationTest;
import ru.hotelbooking.booking.user.Role;
import ru.hotelbooking.booking.user.User;

/**
 * Права доступа к бронированиям, пагинация истории, валидация дат и отмена.
 */
class BookingAccessTest extends BookingIntegrationTest {

    private static final LocalDate START = LocalDate.now().plusDays(30);

    private User ivan;
    private User maria;
    private User admin;

    @BeforeEach
    void givenUsers() {
        ivan = givenUser("ivan", Role.USER);
        maria = givenUser("maria", Role.USER);
        admin = givenUser("admin", Role.ADMIN);
    }

    @Test
    @DisplayName("История содержит только собственные бронирования и поддерживает пагинацию")
    void historyShowsOnlyOwnBookingsWithPaging() throws Exception {
        givenConfirmedBooking(ivan, 1L, 0);
        givenConfirmedBooking(ivan, 2L, 2);
        givenConfirmedBooking(ivan, 3L, 4);
        givenConfirmedBooking(maria, 4L, 0);

        mockMvc.perform(get("/bookings").headers(auth(ivan)).param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].userId").value(everyItem(is(ivan.getId().intValue()))));
    }

    @Test
    @DisplayName("Чужое бронирование выглядит как несуществующее (404), ADMIN видит любое")
    void foreignBookingIsHidden() throws Exception {
        Booking mariasBooking = givenConfirmedBooking(maria, 1L, 0);

        mockMvc.perform(get("/booking/{id}", mariasBooking.getId()).headers(auth(ivan)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/booking/{id}", mariasBooking.getId()).headers(auth(maria)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/booking/{id}", mariasBooking.getId()).headers(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(maria.getId()));
    }

    @Test
    @DisplayName("Дата выезда раньше заезда и заезд в прошлом - 400")
    void invalidDatesAreRejected() throws Exception {
        CreateBookingRequest reversed = new CreateBookingRequest(1L, null, START, START.minusDays(2), false, null);
        CreateBookingRequest past = new CreateBookingRequest(
                1L, null, LocalDate.now().minusDays(5), LocalDate.now().minusDays(3), false, null);

        mockMvc.perform(post("/booking").headers(auth(ivan)).content(json(reversed)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("endDate"));
        mockMvc.perform(post("/booking").headers(auth(ivan)).content(json(past)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Без autoSelect номер обязателен")
    void roomIdIsRequiredForManualBooking() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest(null, null, START, START.plusDays(1), false, null);

        mockMvc.perform(post("/booking").headers(auth(ivan)).content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ROOM_ID_REQUIRED"));
    }

    @Test
    @DisplayName("Отмена подтверждённой брони освобождает слот; повторная отмена ничего не меняет")
    void cancellationReleasesSlotOnce() throws Exception {
        Booking booking = givenConfirmedBooking(ivan, 1L, 0);
        stubRelease(1L);

        mockMvc.perform(delete("/booking/{id}", booking.getId()).headers(auth(ivan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("CANCELLED_BY_USER"));
        mockMvc.perform(delete("/booking/{id}", booking.getId()).headers(auth(ivan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(releasePath(1L))));
    }

    private Booking givenConfirmedBooking(User owner, Long roomId, int shiftDays) {
        LocalDate start = START.plusDays(shiftDays);
        CreateBookingRequest period = new CreateBookingRequest(roomId, 1L, start, start.plusDays(1), false, null);
        Booking booking = Booking.pending(owner, roomId, 1L, period, "seed-" + owner.getUsername() + "-" + roomId,
                Instant.now());
        booking.confirm(1L, Instant.now());
        return bookingRepository.save(booking);
    }
}
