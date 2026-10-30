package ru.hotelbooking.booking.booking.saga;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.hotelbooking.booking.booking.Booking;
import ru.hotelbooking.booking.booking.CancelReason;

/**
 * Доводит до конца саги, которые не завершились сами -
 * повторяет release, если отель был недоступен во время компенсации,
 * и закрывает брони, застрявшие в PENDING (например, сервис перезапустили посреди саги).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaRecoveryJob {

    private final BookingStateService states;
    private final CompensationService compensation;
    private final SagaRecoveryProperties properties;

    @Scheduled(fixedDelayString = "${booking.saga-recovery.interval:30s}",
            initialDelayString = "${booking.saga-recovery.interval:30s}")
    public void recover() {
        retryReleases();
        closeStuckPending();
    }

    private void retryReleases() {
        List<Booking> awaiting = states.findAwaitingRelease(properties.batchSize());
        for (Booking booking : awaiting) {
            if (!compensation.releaseHold(booking)) {
                // отель всё ещё недоступен, остальные попробуем в следующий раз
                return;
            }
        }
    }

    private void closeStuckPending() {
        Instant threshold = Instant.now().minus(properties.pendingTimeout());
        for (Booking booking : states.findPendingCreatedBefore(threshold, properties.batchSize())) {
            log.warn("bookingId={} stuck in PENDING since {}, cancelling", booking.getId(), booking.getCreatedAt());
            compensation.cancelAndRelease(booking.getId(), CancelReason.PROCESSING_TIMEOUT);
        }
    }
}
