package ru.hotelbooking.booking.booking;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.hotelbooking.booking.booking.dto.CreateBookingRequest;
import ru.hotelbooking.booking.support.BookingIntegrationTest;
import ru.hotelbooking.booking.user.Role;
import ru.hotelbooking.booking.user.User;

/**
 * Сценарии саги - успех, отказ отеля, ошибка удалённого сервиса, тайм-аут,
 * разомкнутый circuit breaker и повторная доставка запроса.
 */
class BookingSagaTest extends BookingIntegrationTest {

    private static final long ROOM_ID = 5;
    private static final long HOTEL_ID = 1;
    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(14);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);
    private static final int MAX_ATTEMPTS = 3;

    private User guest;

    @BeforeEach
    void givenGuest() {
        guest = givenUser("ivan", Role.USER);
    }

    @Test
    @DisplayName("Успех: PENDING -> CONFIRMED, в отель уходит requestId и сервисный токен")
    void bookingIsConfirmedWhenHotelHoldsSlot() throws Exception {
        // given
        stubHoldGranted(ROOM_ID, HOTEL_ID);

        // when
        book(manual(ROOM_ID, "key-success"))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.hotelId").value(HOTEL_ID))
                .andExpect(jsonPath("$.nights").value(3))
                .andExpect(jsonPath("$.requestId").value("key-success"));

        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(confirmPath(ROOM_ID)))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo("key-success")))
                .withRequestBody(matchingJsonPath("$.startDate", equalTo(CHECK_IN.toString()))));
    }

    @Test
    @DisplayName("Отель отвечает 409: бронирование CANCELLED, клиент получает 409, компенсация не нужна")
    void bookingIsCancelledWhenRoomIsTaken() throws Exception {
        stubHoldRejected(ROOM_ID);

        book(manual(ROOM_ID, "key-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_AVAILABLE"));

        Booking booking = bookingRepository.findByRequestId("key-conflict").orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelReason()).isEqualTo(CancelReason.ROOM_NOT_AVAILABLE);
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(confirmPath(ROOM_ID))));
        HOTEL.verify(0, postRequestedFor(urlPathEqualTo(releasePath(ROOM_ID))));
    }

    @Test
    @DisplayName("Ошибка 500: ограниченные повторы, затем CANCELLED и release")
    void serverErrorIsRetriedThenCompensated() throws Exception {
        HOTEL.stubFor(post(urlPathEqualTo(confirmPath(ROOM_ID))).willReturn(aResponse().withStatus(500)));
        stubRelease(ROOM_ID);

        book(manual(ROOM_ID, "key-500"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("HOTEL_SERVICE_UNAVAILABLE"));

        HOTEL.verify(MAX_ATTEMPTS, postRequestedFor(urlPathEqualTo(confirmPath(ROOM_ID))));
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(releasePath(ROOM_ID)))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo("key-500"))));
        assertCancelledByHotelFailure("key-500");
    }

    @Test
    @DisplayName("Тайм-аут: отель отвечает дольше read-timeout, после повторов - компенсация")
    void timeoutIsRetriedThenCompensated() throws Exception {
        HOTEL.stubFor(post(urlPathEqualTo(confirmPath(ROOM_ID)))
                .willReturn(okJson("{\"roomId\": 5, \"hotelId\": 1}").withFixedDelay(1500)));
        stubRelease(ROOM_ID);

        book(manual(ROOM_ID, "key-timeout"))
                .andExpect(status().isServiceUnavailable());

        HOTEL.verify(MAX_ATTEMPTS, postRequestedFor(urlPathEqualTo(confirmPath(ROOM_ID))));
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(releasePath(ROOM_ID))));
        assertCancelledByHotelFailure("key-timeout");
    }

    @Test
    @DisplayName("Circuit breaker разомкнут: в отель не уходит ни одного запроса, бронь отменяется")
    void openCircuitCancelsBookingWithoutRemoteCalls() throws Exception {
        // given
        circuitBreakerRegistry.circuitBreaker("hotel").transitionToForcedOpenState();

        // when
        book(manual(ROOM_ID, "key-open-circuit")).andExpect(status().isServiceUnavailable());

        // then
        Booking booking = bookingRepository.findByRequestId("key-open-circuit").orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelReason()).isEqualTo(CancelReason.HOTEL_UNAVAILABLE);
        HOTEL.verify(0, postRequestedFor(urlPathEqualTo(confirmPath(ROOM_ID))));
    }

    @Test
    @DisplayName("Повторная доставка с тем же requestId возвращает ту же бронь (200) без нового вызова отеля")
    void duplicateDeliveryIsIdempotent() throws Exception {
        stubHoldGranted(ROOM_ID, HOTEL_ID);

        String first = book(manual(ROOM_ID, "key-duplicate"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String second = book(manual(ROOM_ID, "key-duplicate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(second).path("id")).isEqualTo(objectMapper.readTree(first).path("id"));
        assertThat(bookingRepository.count()).isEqualTo(1);
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(confirmPath(ROOM_ID))));
    }

    @Test
    @DisplayName("Автоподбор: номера сортируются по times_booked, занятый кандидат пропускается")
    void autoSelectFallsBackToNextCandidate() throws Exception {
        // given: отель вернул кандидатов не в том порядке
        HOTEL.stubFor(get(urlPathEqualTo("/api/rooms/recommend")).willReturn(okJson("""
                [
                  {"id": 9, "hotelId": 1, "available": true, "timesBooked": 4},
                  {"id": 7, "hotelId": 1, "available": true, "timesBooked": 0},
                  {"id": 3, "hotelId": 1, "available": true, "timesBooked": 0}
                ]
                """)));
        stubHoldRejected(3);
        stubHoldGranted(7, HOTEL_ID);

        // when / then
        book(autoSelect("key-auto"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(7));

        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(confirmPath(3))));
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(confirmPath(7))));
        HOTEL.verify(0, postRequestedFor(urlPathEqualTo(confirmPath(9))));
    }

    private void assertCancelledByHotelFailure(String requestId) {
        Booking booking = bookingRepository.findByRequestId(requestId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelReason()).isEqualTo(CancelReason.HOTEL_UNAVAILABLE);
    }

    private ResultActions book(CreateBookingRequest request) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/booking").headers(auth(guest))
                .content(json(request)));
    }

    private static CreateBookingRequest manual(long roomId, String requestId) {
        return new CreateBookingRequest(roomId, null, CHECK_IN, CHECK_OUT, false, requestId);
    }

    private static CreateBookingRequest autoSelect(String requestId) {
        return new CreateBookingRequest(null, HOTEL_ID, CHECK_IN, CHECK_OUT, true, requestId);
    }
}
