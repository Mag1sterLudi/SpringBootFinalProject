package ru.hotelbooking.hotel.reservation;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.common.error.ConflictException;
import ru.hotelbooking.common.error.NotFoundException;
import ru.hotelbooking.hotel.reservation.dto.AvailabilityRequest;
import ru.hotelbooking.hotel.reservation.dto.ReleaseRequest;
import ru.hotelbooking.hotel.reservation.dto.ReleaseResponse;
import ru.hotelbooking.hotel.reservation.dto.ReleaseResponse.ReleaseOutcome;
import ru.hotelbooking.hotel.reservation.dto.ReservationResponse;
import ru.hotelbooking.hotel.room.Room;
import ru.hotelbooking.hotel.room.RoomRepository;

/**
 * Сначала берём SELECT ... FOR UPDATE на строку номера, поэтому параллельные запросы
 * на один номер выполняются по очереди и двойного бронирования не будет.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;

    @Override
    @Transactional
    public ReservationResponse confirmAvailability(Long roomId, AvailabilityRequest request) {
        Room room = lockRoom(roomId);
        Optional<RoomReservation> existing = reservationRepository.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            return replayConfirmation(existing.get(), roomId);
        }
        ensureBookable(room, request);

        RoomReservation reservation = reservationRepository.save(RoomReservation.hold(room, request, Instant.now()));
        room.registerBooking();
        log.info("Slot HELD: bookingId={}, requestId={}, roomId={}, period=[{}, {}), timesBooked={}",
                request.bookingId(), request.requestId(), roomId, request.startDate(), request.endDate(),
                room.getTimesBooked());
        return toResponse(reservation);
    }

    @Override
    @Transactional
    public ReleaseResponse release(Long roomId, ReleaseRequest request) {
        Room room = lockRoom(roomId);
        Optional<RoomReservation> existing = reservationRepository.findByRequestId(request.requestId());
        if (existing.isEmpty()) {
            log.info("Nothing to release: bookingId={}, requestId={}, roomId={}",
                    request.bookingId(), request.requestId(), roomId);
            return new ReleaseResponse(request.requestId(), roomId, ReleaseOutcome.NOTHING_TO_RELEASE);
        }

        RoomReservation reservation = existing.get();
        ensureSameRoom(reservation, roomId);
        if (!reservation.isHeld()) {
            log.info("Duplicate release ignored: bookingId={}, requestId={}", request.bookingId(), request.requestId());
            return new ReleaseResponse(request.requestId(), roomId, ReleaseOutcome.ALREADY_RELEASED);
        }
        reservation.release(Instant.now());
        room.unregisterBooking();
        log.info("Slot RELEASED: bookingId={}, requestId={}, roomId={}, timesBooked={}",
                request.bookingId(), request.requestId(), roomId, room.getTimesBooked());
        return new ReleaseResponse(request.requestId(), roomId, ReleaseOutcome.RELEASED);
    }

    private ReservationResponse replayConfirmation(RoomReservation reservation, Long roomId) {
        ensureSameRoom(reservation, roomId);
        if (!reservation.isHeld()) {
            throw new ConflictException("SLOT_ALREADY_RELEASED",
                    "Request " + reservation.getRequestId() + " was already compensated");
        }
        log.info("Duplicate confirm, returning existing hold: bookingId={}, requestId={}, reservationId={}",
                reservation.getBookingId(), reservation.getRequestId(), reservation.getId());
        return toResponse(reservation);
    }

    private void ensureBookable(Room room, AvailabilityRequest request) {
        if (!room.isAvailable()) {
            throw new ConflictException("ROOM_OUT_OF_SERVICE", "Room " + room.getId() + " is out of service");
        }
        if (reservationRepository.existsActiveOverlap(room.getId(), request.startDate(), request.endDate())) {
            log.info("Slot conflict: bookingId={}, requestId={}, roomId={}, period=[{}, {})",
                    request.bookingId(), request.requestId(), room.getId(), request.startDate(), request.endDate());
            throw new ConflictException("ROOM_NOT_AVAILABLE",
                    "Room " + room.getId() + " is already booked for the requested dates");
        }
    }

    private static void ensureSameRoom(RoomReservation reservation, Long roomId) {
        if (!reservation.belongsToRoom(roomId)) {
            throw new ConflictException("REQUEST_ID_REUSED",
                    "Request " + reservation.getRequestId() + " belongs to another room");
        }
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId).orElseThrow(() -> NotFoundException.of("Room", roomId));
    }

    private static ReservationResponse toResponse(RoomReservation reservation) {
        Room room = reservation.getRoom();
        return new ReservationResponse(
                reservation.getId(),
                room.getId(),
                room.getHotel().getId(),
                reservation.getRequestId(),
                reservation.getBookingId(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getStatus());
    }
}
