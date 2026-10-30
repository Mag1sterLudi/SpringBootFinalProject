package ru.hotelbooking.hotel.hotel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import ru.hotelbooking.common.security.JwtProperties;
import ru.hotelbooking.hotel.hotel.dto.HotelRequest;
import ru.hotelbooking.hotel.support.IntegrationTest;
import ru.hotelbooking.hotel.support.TestTokens;

class HotelApiTest extends IntegrationTest {

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("ADMIN создаёт отель - 201")
    void adminCreatesHotel() throws Exception {
        // given
        HotelRequest request = new HotelRequest("Белые ночи", "Санкт-Петербург", "Литейный, 5", 4, null);

        // when / then
        mockMvc.perform(post("/api/hotels").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Белые ночи"))
                .andExpect(jsonPath("$.stars").value(4));
    }

    @Test
    @DisplayName("USER не может создавать отели - 403")
    void userCannotCreateHotel() throws Exception {
        HotelRequest request = new HotelRequest("Белые ночи", "Санкт-Петербург", "Литейный, 5", 4, null);

        mockMvc.perform(post("/api/hotels").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Без токена - 401 в формате ApiError")
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/hotels"));
    }

    @Test
    @DisplayName("Некорректные поля отеля - 400 со списком нарушений")
    void invalidHotelIsRejected() throws Exception {
        HotelRequest request = new HotelRequest("", "Казань", "ул. Баумана, 1", 7, null);

        mockMvc.perform(post("/api/hotels").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations.length()").value(2));
    }

    @Test
    @DisplayName("Список отелей фильтруется по городу без учёта регистра")
    void hotelsAreFilteredByCity() throws Exception {
        givenHotel("Волга", "Казань");
        givenHotel("Нева", "Санкт-Петербург");

        mockMvc.perform(get("/api/hotels").param("city", "казань").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Волга"));
    }

    @Test
    @DisplayName("Сервис сам проверяет подпись и срок действия JWT")
    void serviceValidatesRealTokens() throws Exception {
        Instant now = Instant.now();
        String valid = TestTokens.issue(jwtProperties, "USER", now, now.plus(1, ChronoUnit.HOURS));
        String expired = TestTokens.issue(jwtProperties, "USER",
                now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/hotels").header(HttpHeaders.AUTHORIZATION, "Bearer " + valid))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/hotels").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }
}
