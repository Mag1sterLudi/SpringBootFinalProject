package ru.hotelbooking.booking.booking.saga;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.booking.booking.Booking;
import ru.hotelbooking.booking.booking.BookingRepository;
import ru.hotelbooking.booking.booking.BookingStatus;
import ru.hotelbooking.booking.booking.CancelReason;
import ru.hotelbooking.booking.booking.RoomCandidate;
import ru.hotelbooking.booking.user.User;
import ru.hotelbooking.booking.user.UserRepository;
import ru.hotelbooking.common.error.NotFoundException;
import ru.hotelbooking.common.validation.DateRange;

/**
 * Шаги саги, каждый в своей локальной транзакции. Переходы статусов логируются с bookingId.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingStateService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<Booking> findByRequestId(String requestId) {
        return bookingRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public Booking get(Long bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow(() -> NotFoundException.of("Booking", bookingId));
    }

    @Transactional
    public Booking createPending(Long userId, DateRange period, RoomCandidate room, String requestId) {
        User user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
        Booking booking = bookingRepository.saveAndFlush(
                Booking.pending(user, room.roomId(), room.hotelId(), period, requestId, Instant.now()));
        log.info("bookingId={} state=PENDING requestId={} userId={} roomId={} period=[{}, {})",
                booking.getId(), requestId, userId, room.roomId(), period.startDate(), period.endDate());
        return booking;
    }

    @Transactional
    public Booking assignRoom(Long bookingId, Long roomId) {
        Booking booking = get(bookingId);
        if (!roomId.equals(booking.getRoomId())) {
            log.info("bookingId={} trying next candidate: roomId {} -> {}", bookingId, booking.getRoomId(), roomId);
            booking.assignRoom(roomId, Instant.now());
        }
        return booking;
    }

    @Transactional
    public Booking confirm(Long bookingId, Long hotelId) {
        Booking booking = get(bookingId);
        booking.confirm(hotelId, Instant.now());
        log.info("bookingId={} transition PENDING -> CONFIRMED roomId={} hotelId={}",
                bookingId, booking.getRoomId(), booking.getHotelId());
        return booking;
    }

    @Transactional
    public Booking cancel(Long bookingId, CancelReason reason) {
        Booking booking = get(bookingId);
        BookingStatus previous = booking.getStatus();
        if (booking.cancel(reason, Instant.now())) {
            log.info("bookingId={} transition {} -> CANCELLED reason={}", bookingId, previous, reason);
        }
        return booking;
    }

    // отмена и отметка "снять слот в отеле" в одной транзакции: если сервис упадёт до release,
    // SagaRecoveryJob увидит флаг и доделает компенсацию
    @Transactional
    public Booking cancelAwaitingRelease(Long bookingId, CancelReason reason) {
        Booking booking = cancel(bookingId, reason);
        booking.awaitRelease(Instant.now());
        return booking;
    }

    @Transactional
    public void markReleased(Long bookingId) {
        get(bookingId).releaseConfirmed(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<Booking> findAwaitingRelease(int limit) {
        return bookingRepository.findByReleasePendingTrueOrderByUpdatedAtAsc(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<Booking> findPendingCreatedBefore(Instant createdBefore, int limit) {
        return bookingRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                BookingStatus.PENDING, createdBefore, PageRequest.of(0, limit));
    }
}
