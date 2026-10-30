package ru.hotelbooking.booking.booking.saga;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.hotelbooking.booking.booking.Booking;
import ru.hotelbooking.booking.booking.CancelReason;
import ru.hotelbooking.booking.client.HotelClient;
import ru.hotelbooking.booking.client.HotelServiceUnavailableException;
import ru.hotelbooking.booking.client.dto.ReleaseRequest;
import ru.hotelbooking.booking.client.dto.ReleaseResult;
import ru.hotelbooking.common.error.NotFoundException;

/**
 * Компенсация. Отменить бронь и снять блокировку в отеле. release можно вызывать повторно -
 * второй вызов ничего не меняет.
 * Если отель недоступен, бронь остаётся с release_pending = true и её подхватит SagaRecoveryJob.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationService {

    private final HotelClient hotelClient;
    private final BookingStateService states;

    public Booking cancelAndRelease(Long bookingId, CancelReason reason) {
        Booking cancelled = states.cancelAwaitingRelease(bookingId, reason);
        releaseHold(cancelled);
        return cancelled;
    }

    public boolean releaseHold(Booking booking) {
        ReleaseRequest request = new ReleaseRequest(
                booking.getRequestId(), booking.getId(), booking.getStartDate(), booking.getEndDate());
        try {
            ReleaseResult result = hotelClient.release(booking.getRoomId(), request);
            log.info("bookingId={} compensation done: roomId={} outcome={}",
                    booking.getId(), booking.getRoomId(), result.outcome());
        } catch (NotFoundException ex) {
            log.warn("bookingId={} compensation skipped: room {} not found", booking.getId(), booking.getRoomId());
        } catch (HotelServiceUnavailableException | CallNotPermittedException ex) {
            log.warn("bookingId={} compensation postponed, roomId={} requestId={}: {}",
                    booking.getId(), booking.getRoomId(), booking.getRequestId(), ex.getMessage());
            return false;
        }
        states.markReleased(booking.getId());
        return true;
    }
}
