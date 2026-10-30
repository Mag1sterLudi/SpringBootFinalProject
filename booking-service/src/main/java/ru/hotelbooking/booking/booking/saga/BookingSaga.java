package ru.hotelbooking.booking.booking.saga;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.hotelbooking.booking.booking.Booking;
import ru.hotelbooking.booking.booking.CancelReason;
import ru.hotelbooking.booking.booking.RoomCandidate;
import ru.hotelbooking.booking.client.HotelClient;
import ru.hotelbooking.booking.client.HotelServiceUnavailableException;
import ru.hotelbooking.booking.client.RoomUnavailableException;
import ru.hotelbooking.booking.client.dto.HoldRequest;
import ru.hotelbooking.booking.client.dto.HoldResponse;
import ru.hotelbooking.common.error.ApiException;
import ru.hotelbooking.common.error.NotFoundException;

/**
 * Сага бронирования. Отель занял слот - CONFIRMED. Отель ответил 409 - пробуем следующего
 * кандидата, а если кандидатов не осталось - CANCELLED. Тайм-аут, 5xx после повторов или
 * открытый CircuitBreaker - CANCELLED и release.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSaga {

    private final HotelClient hotelClient;
    private final BookingStateService states;
    private final CompensationService compensation;

    public Booking execute(Booking pending, List<RoomCandidate> candidates) {
        Long bookingId = pending.getId();
        for (RoomCandidate candidate : candidates) {
            Optional<Booking> confirmed = tryReserve(bookingId, candidate);
            if (confirmed.isPresent()) {
                return confirmed.get();
            }
        }
        states.cancel(bookingId, CancelReason.ROOM_NOT_AVAILABLE);
        throw new RoomUnavailableException("No free room for booking " + bookingId + " on the requested dates");
    }

    private Optional<Booking> tryReserve(Long bookingId, RoomCandidate candidate) {
        Booking booking = states.assignRoom(bookingId, candidate.roomId());
        try {
            HoldResponse hold = hotelClient.confirmAvailability(candidate.roomId(), holdRequest(booking));
            log.info("bookingId={} slot held: roomId={} reservationId={}",
                    bookingId, hold.roomId(), hold.reservationId());
            return Optional.of(states.confirm(bookingId, hold.hotelId()));
        } catch (RoomUnavailableException ex) {
            log.info("bookingId={} roomId={} rejected by hotel: {}", bookingId, candidate.roomId(), ex.getMessage());
            return Optional.empty();
        } catch (HotelServiceUnavailableException | CallNotPermittedException ex) {
            log.warn("bookingId={} hotel unavailable, starting compensation: {}", bookingId, ex.getMessage());
            compensation.cancelAndRelease(bookingId, CancelReason.HOTEL_UNAVAILABLE);
            throw new HotelServiceUnavailableException(
                    "Booking " + bookingId + " was cancelled: hotel service is unavailable", ex);
        } catch (NotFoundException ex) {
            states.cancel(bookingId, CancelReason.ROOM_NOT_FOUND);
            throw ex;
        } catch (ApiException ex) {
            states.cancel(bookingId, CancelReason.REJECTED_BY_HOTEL);
            throw ex;
        }
    }

    private static HoldRequest holdRequest(Booking booking) {
        return new HoldRequest(booking.getRequestId(), booking.getId(), booking.getStartDate(), booking.getEndDate());
    }
}
