package ru.hotelbooking.hotel.reservation;

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
import java.util.function.IntFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hotelbooking.common.error.ConflictException;
import ru.hotelbooking.hotel.reservation.dto.AvailabilityRequest;
import ru.hotelbooking.hotel.reservation.dto.ReservationResponse;
import ru.hotelbooking.hotel.room.Room;
import ru.hotelbooking.hotel.support.IntegrationTest;

/**
 * Конкурентные попытки занять один и тот же номер на одни и те же даты.
 */
class ConcurrentHoldTest extends IntegrationTest {

    private static final int THREADS = 8;
    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(20);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(4);

    @Autowired
    private AvailabilityService availabilityService;

    private Room room;

    @BeforeEach
    void givenRoom() {
        room = givenRoom(givenHotel("Волга", "Казань"), "101", 0);
    }

    @Test
    @DisplayName("Из параллельных запросов разных клиентов слот получает ровно один, остальные - конфликт")
    void onlyOneOfConcurrentRequestsWins() throws Exception {
        List<Outcome> outcomes = runConcurrently(
                i -> new AvailabilityRequest("req-" + i, (long) i, CHECK_IN, CHECK_OUT));

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(Outcome::conflicted).hasSize(THREADS - 1);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1);
    }

    @Test
    @DisplayName("Параллельная повторная доставка одного requestId создаёт один слот")
    void concurrentDuplicatesCreateSingleHold() throws Exception {
        List<Outcome> outcomes = runConcurrently(i -> new AvailabilityRequest("same-request", 1L, CHECK_IN, CHECK_OUT));

        assertThat(outcomes).allMatch(Outcome::succeeded);
        assertThat(outcomes).extracting(outcome -> outcome.response().reservationId()).containsOnly(
                outcomes.get(0).response().reservationId());
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1);
    }

    private List<Outcome> runConcurrently(IntFunction<AvailabilityRequest> requestFactory) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                AvailabilityRequest request = requestFactory.apply(i);
                Callable<Outcome> task = () -> {
                    start.await();
                    return attempt(request);
                };
                futures.add(executor.submit(task));
            }
            start.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private Outcome attempt(AvailabilityRequest request) {
        try {
            return new Outcome(availabilityService.confirmAvailability(room.getId(), request), null);
        } catch (ConflictException ex) {
            return new Outcome(null, ex);
        }
    }

    private record Outcome(ReservationResponse response, ConflictException conflict) {

        boolean succeeded() {
            return response != null;
        }

        boolean conflicted() {
            return conflict != null;
        }
    }
}
