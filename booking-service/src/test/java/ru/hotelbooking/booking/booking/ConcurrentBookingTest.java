package ru.hotelbooking.booking.booking;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hotelbooking.booking.booking.dto.CreateBookingRequest;
import ru.hotelbooking.booking.security.CurrentUser;
import ru.hotelbooking.booking.support.BookingIntegrationTest;
import ru.hotelbooking.booking.user.Role;
import ru.hotelbooking.booking.user.User;

/**
 * Клиент не дождался ответа и отправил тот же запрос ещё несколько раз параллельно.
 */
class ConcurrentBookingTest extends BookingIntegrationTest {

    private static final int THREADS = 6;
    private static final long ROOM_ID = 11;

    @Autowired
    private BookingService bookingService;

    @Test
    @DisplayName("Параллельные запросы с одним ключом повторного запроса создают одну бронь и один вызов отеля")
    void concurrentDuplicatesCreateSingleBooking() throws Exception {
        // given
        stubHoldGranted(ROOM_ID, 1);
        User user = givenUser("ivan", Role.USER);
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), Role.USER);
        LocalDate start = LocalDate.now().plusDays(40);
        CreateBookingRequest request =
                new CreateBookingRequest(ROOM_ID, null, start, start.plusDays(2), false, "same-key");

        // when
        List<BookingResult> results = runConcurrently(() -> bookingService.create(currentUser, request));

        // then
        assertThat(results).extracting(result -> result.booking().id()).containsOnly(results.get(0).booking().id());
        assertThat(results).filteredOn(BookingResult::created).hasSize(1);
        assertThat(bookingRepository.count()).isEqualTo(1);
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(confirmPath(ROOM_ID))));
    }

    private List<BookingResult> runConcurrently(Callable<BookingResult> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<BookingResult>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return action.call();
                }));
            }
            start.countDown();
            List<BookingResult> results = new ArrayList<>();
            for (Future<BookingResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
