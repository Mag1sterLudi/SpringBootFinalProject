package ru.hotelbooking.booking.booking;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.hotelbooking.booking.booking.dto.CreateBookingRequest;
import ru.hotelbooking.booking.booking.saga.SagaRecoveryJob;
import ru.hotelbooking.booking.support.BookingIntegrationTest;
import ru.hotelbooking.booking.user.Role;
import ru.hotelbooking.booking.user.User;

/**
 * Саги, которые не смогли завершиться сразу, доводит до конца SagaRecoveryJob.
 */
class SagaRecoveryTest extends BookingIntegrationTest {

    private static final long ROOM_ID = 21;
    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(50);

    @Autowired
    private SagaRecoveryJob recoveryJob;

    private User guest;

    @BeforeEach
    void givenGuest() {
        guest = givenUser("ivan", Role.USER);
    }

    @Test
    @DisplayName("release не прошёл во время компенсации - задача повторяет его, когда отель вернулся")
    void failedReleaseIsRetriedLater() throws Exception {
        // given: отель недоступен и для confirm, и для release
        HOTEL.stubFor(post(urlPathEqualTo(confirmPath(ROOM_ID))).willReturn(aResponse().withStatus(500)));
        HOTEL.stubFor(post(urlPathEqualTo(releasePath(ROOM_ID))).willReturn(aResponse().withStatus(500)));
        CreateBookingRequest request =
                new CreateBookingRequest(ROOM_ID, null, CHECK_IN, CHECK_IN.plusDays(2), false, "key-retry");
        mockMvc.perform(MockMvcRequestBuilders.post("/booking").headers(auth(guest)).content(json(request)))
                .andExpect(status().isServiceUnavailable());
        assertThat(booking("key-retry").isReleasePending()).isTrue();

        // when: отель снова отвечает
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        HOTEL.resetAll();
        stubRelease(ROOM_ID);
        recoveryJob.recover();

        // then
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(releasePath(ROOM_ID)))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo("key-retry"))));
        assertThat(booking("key-retry").isReleasePending()).isFalse();
        assertThat(booking("key-retry").getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Бронь, зависшая в PENDING дольше тайм-аута, отменяется со снятием слота")
    void stuckPendingBookingIsCancelled() {
        // given
        Instant longAgo = Instant.now().minus(Duration.ofHours(1));
        CreateBookingRequest period =
                new CreateBookingRequest(ROOM_ID, 1L, CHECK_IN, CHECK_IN.plusDays(1), false, null);
        bookingRepository.save(Booking.pending(guest, ROOM_ID, 1L, period, "key-stuck", longAgo));
        stubRelease(ROOM_ID);

        // when
        recoveryJob.recover();

        // then
        Booking booking = booking("key-stuck");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelReason()).isEqualTo(CancelReason.PROCESSING_TIMEOUT);
        assertThat(booking.isReleasePending()).isFalse();
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(releasePath(ROOM_ID))));
    }

    @Test
    @DisplayName("Свежая PENDING-бронь (сага ещё идёт) задачей не трогается")
    void freshPendingBookingIsLeftAlone() {
        CreateBookingRequest period =
                new CreateBookingRequest(ROOM_ID, 1L, CHECK_IN, CHECK_IN.plusDays(1), false, null);
        bookingRepository.save(Booking.pending(guest, ROOM_ID, 1L, period, "key-fresh", Instant.now()));

        recoveryJob.recover();

        assertThat(booking("key-fresh").getStatus()).isEqualTo(BookingStatus.PENDING);
        HOTEL.verify(0, postRequestedFor(urlPathEqualTo(releasePath(ROOM_ID))));
    }

    private Booking booking(String requestId) {
        return bookingRepository.findByRequestId(requestId).orElseThrow();
    }
}
