package ru.hotelbooking.booking.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.hotelbooking.booking.booking.Booking;
import ru.hotelbooking.booking.booking.dto.CreateBookingRequest;
import ru.hotelbooking.booking.support.BookingIntegrationTest;
import ru.hotelbooking.booking.user.dto.CreateUserRequest;
import ru.hotelbooking.booking.user.dto.UpdateUserRequest;

class UserAdminApiTest extends BookingIntegrationTest {

    private User admin;
    private User user;

    @BeforeEach
    void givenAccounts() {
        admin = givenUser("admin", Role.ADMIN);
        user = givenUser("ivan", Role.USER);
    }

    @Test
    @DisplayName("Администрирование пользователей недоступно роли USER - 403")
    void userCannotManageUsers() throws Exception {
        mockMvc.perform(get("/user").headers(auth(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN создаёт пользователя с ролью и меняет её через PATCH")
    void adminCreatesAndUpdatesUser() throws Exception {
        String created = mockMvc.perform(post("/user").headers(auth(admin))
                        .content(json(new CreateUserRequest("olga", "olga-password", Role.USER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("id").asLong();

        mockMvc.perform(patch("/user/{id}", id).headers(auth(admin))
                        .content(json(new UpdateUserRequest(null, null, Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("olga"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Пользователь без бронирований удаляется, с бронированиями - 409")
    void deleteRespectsBookings() throws Exception {
        User withBooking = givenUser("maria", Role.USER);
        LocalDate start = LocalDate.now().plusDays(3);
        CreateBookingRequest period = new CreateBookingRequest(1L, 1L, start, start.plusDays(1), false, null);
        bookingRepository.save(Booking.pending(withBooking, 1L, 1L, period, "user-test", Instant.now()));

        mockMvc.perform(delete("/user/{id}", user.getId()).headers(auth(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/user/{id}", withBooking.getId()).headers(auth(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_HAS_BOOKINGS"));
    }
}
