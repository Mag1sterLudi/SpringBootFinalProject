package ru.hotelbooking.booking.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotelbooking.booking.booking.BookingRepository;
import ru.hotelbooking.booking.security.TokenService;
import ru.hotelbooking.booking.user.Role;
import ru.hotelbooking.booking.user.User;
import ru.hotelbooking.booking.user.UserRepository;

/**
 * Вместо hotel-service - WireMock на случайном порту. Он регистрируется в SimpleDiscoveryClient
 * под именем hotel-service, так что в тестах работает настоящий клиент с балансировщиком,
 * тайм-аутами, Retry и CircuitBreaker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BookingIntegrationTest {

    protected static final WireMockServer HOTEL = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        HOTEL.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TokenService tokenService;

    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void hotelServiceInstance(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.hotel-service[0].uri",
                () -> "http://localhost:" + HOTEL.port());
    }

    @BeforeEach
    void resetState() {
        HOTEL.resetAll();
        bookingRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    protected User givenUser(String username, Role role) {
        return userRepository.save(new User(username, passwordEncoder.encode("secret123"), role));
    }

    protected HttpHeaders auth(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenService.issueFor(user).accessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    protected static void stubHoldGranted(long roomId, long hotelId) {
        HOTEL.stubFor(post(urlPathEqualTo(confirmPath(roomId)))
                .willReturn(okJson("""
                        {"reservationId": 100, "roomId": %d, "hotelId": %d, "status": "HELD"}
                        """.formatted(roomId, hotelId))));
    }

    protected static void stubHoldRejected(long roomId) {
        HOTEL.stubFor(post(urlPathEqualTo(confirmPath(roomId)))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"code\": \"ROOM_NOT_AVAILABLE\", \"message\": \"Room is already booked\"}")));
    }

    protected static void stubRelease(long roomId) {
        HOTEL.stubFor(post(urlPathEqualTo(releasePath(roomId)))
                .willReturn(okJson("{\"roomId\": %d, \"outcome\": \"RELEASED\"}".formatted(roomId))));
    }

    protected static String confirmPath(long roomId) {
        return "/api/rooms/" + roomId + "/confirm-availability";
    }

    protected static String releasePath(long roomId) {
        return "/api/rooms/" + roomId + "/release";
    }
}
