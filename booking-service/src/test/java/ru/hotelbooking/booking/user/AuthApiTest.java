package ru.hotelbooking.booking.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import ru.hotelbooking.booking.support.BookingIntegrationTest;
import ru.hotelbooking.booking.user.dto.LoginRequest;
import ru.hotelbooking.booking.user.dto.RegisterRequest;

class AuthApiTest extends BookingIntegrationTest {

    @Test
    @DisplayName("Регистрация выдаёт токен со сроком действия 1 час, токен принимается сервисом")
    void registerIssuesWorkingToken() throws Exception {
        // when
        String body = mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("petr", "petr-password"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).path("accessToken").asText();

        // then
        mockMvc.perform(get("/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        User saved = userRepository.findByUsername("petr").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("petr-password").startsWith("$2");
    }

    @Test
    @DisplayName("Слишком короткий пароль и недопустимые символы в логине - 400")
    void invalidRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("пётр!", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[?(@.field == 'password')]").exists());
    }

    @Test
    @DisplayName("Неверный пароль - 401 без уточнения, что именно неверно")
    void wrongPasswordIsUnauthorized() throws Exception {
        givenUser("boss", Role.ADMIN);

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("boss", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    @DisplayName("Защищённый эндпойнт без токена и с поддельным токеном - 401")
    void protectedEndpointRequiresValidToken() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
