package ru.hotelbooking.booking.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.hotelbooking.booking.booking.dto.BookingResponse;
import ru.hotelbooking.booking.booking.dto.CreateBookingRequest;
import ru.hotelbooking.booking.booking.saga.BookingSaga;
import ru.hotelbooking.booking.booking.saga.BookingStateService;
import ru.hotelbooking.booking.booking.saga.CompensationService;
import ru.hotelbooking.booking.client.RoomUnavailableException;
import ru.hotelbooking.booking.client.dto.RoomView;
import ru.hotelbooking.booking.security.CurrentUser;
import ru.hotelbooking.common.error.BadRequestException;
import ru.hotelbooking.common.error.ConflictException;
import ru.hotelbooking.common.error.NotFoundException;
import ru.hotelbooking.common.web.PageResponse;

/**
 * Фасад сценариев бронирования. Сам транзакций не открывает. Каждый шаг саги
 * выполняется в BookingStateService в собственной локальной транзакции,
 * а удалённые вызовы происходят между ними, не удерживая соединение с БД.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Pattern REQUEST_ID_FORMAT = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");
    private static final int CONCURRENT_LOOKUP_ATTEMPTS = 20;
    private static final long CONCURRENT_LOOKUP_PAUSE_MS = 50;

    private final BookingRepository bookingRepository;
    private final BookingStateService states;
    private final BookingSaga saga;
    private final CompensationService compensation;
    private final RoomRecommendationService recommendations;
    private final BookingMapper mapper;

    @Override
    public BookingResult create(CurrentUser user, CreateBookingRequest request) {
        String requestId = resolveRequestId(request.requestId());
        Optional<Booking> existing = states.findByRequestId(requestId);
        if (existing.isPresent()) {
            return replay(existing.get(), user);
        }

        List<RoomCandidate> candidates;
        try {
            candidates = candidatesFor(request);
        } catch (RoomUnavailableException ex) {
            // номер мог занять параллельный дубль этого же запроса, тогда это не конфликт, а повтор
            return states.findByRequestId(requestId).map(booking -> replay(booking, user)).orElseThrow(() -> ex);
        }
        Booking pending;
        try {
            pending = states.createPending(user.id(), request, candidates.get(0), requestId);
        } catch (DataAccessException ex) {
            // параллельный запрос с тем же requestId вставляет такую же запись
            return replay(awaitConcurrentBooking(requestId, ex), user);
        }
        return BookingResult.newlyCreated(mapper.toResponse(saga.execute(pending, candidates)));
    }

    @Override
    public PageResponse<BookingResponse> history(CurrentUser user, BookingStatus status, Pageable pageable) {
        Page<Booking> page = status == null
                ? bookingRepository.findByUserId(user.id(), pageable)
                : bookingRepository.findByUserIdAndStatus(user.id(), status, pageable);
        return PageResponse.from(page.map(mapper::toResponse));
    }

    @Override
    public BookingResponse get(CurrentUser user, Long bookingId) {
        return mapper.toResponse(findAccessible(user, bookingId));
    }

    @Override
    public BookingResponse cancel(CurrentUser user, Long bookingId) {
        Booking booking = findAccessible(user, bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return mapper.toResponse(booking);
        }
        if (booking.getStatus() == BookingStatus.PENDING) {
            throw new ConflictException("BOOKING_IN_PROGRESS", "Booking " + bookingId + " is still being processed");
        }
        if (!booking.getStartDate().isAfter(LocalDate.now())) {
            throw new ConflictException("BOOKING_ALREADY_STARTED", "Booking " + bookingId + " has already started");
        }
        log.info("bookingId={} cancellation requested by userId={}", bookingId, user.id());
        return mapper.toResponse(compensation.cancelAndRelease(bookingId, CancelReason.CANCELLED_BY_USER));
    }

    private List<RoomCandidate> candidatesFor(CreateBookingRequest request) {
        if (request.autoSelect()) {
            List<RoomView> rooms = recommendations.recommend(request.hotelId(), request.startDate(), request.endDate());
            if (rooms.isEmpty()) {
                throw new RoomUnavailableException("No free rooms for the requested dates");
            }
            return rooms.stream().map(RoomCandidate::of).toList();
        }
        if (request.roomId() == null) {
            throw new BadRequestException("ROOM_ID_REQUIRED", "roomId is required when autoSelect is false");
        }
        if (bookingRepository.existsOverlapping(request.roomId(), request.startDate(), request.endDate(),
                BookingStatus.ACTIVE)) {
            throw new RoomUnavailableException(
                    "Room " + request.roomId() + " is already booked for the requested dates");
        }
        return List.of(new RoomCandidate(request.roomId(), request.hotelId()));
    }

    private BookingResult replay(Booking booking, CurrentUser user) {
        if (!booking.isOwnedBy(user.id())) {
            throw new ConflictException("IDEMPOTENCY_KEY_REUSED",
                    "Idempotency key is already used by another request");
        }
        log.info("bookingId={} duplicate delivery of requestId={}, returning stored state {}",
                booking.getId(), booking.getRequestId(), booking.getStatus());
        return BookingResult.replayed(mapper.toResponse(booking));
    }

    private Booking findAccessible(CurrentUser user, Long bookingId) {
        return bookingRepository.findById(bookingId)
                .filter(booking -> user.isAdmin() || booking.isOwnedBy(user.id()))
                .orElseThrow(() -> NotFoundException.of("Booking", bookingId));
    }

    private Booking awaitConcurrentBooking(String requestId, DataAccessException cause) {
        // H2 сразу бросает ошибку, если дубликат ключа ещё не закоммичен, поэтому ждём коммита соседа
        for (int attempt = 0; attempt < CONCURRENT_LOOKUP_ATTEMPTS; attempt++) {
            Optional<Booking> booking = states.findByRequestId(requestId);
            if (booking.isPresent()) {
                return booking.get();
            }
            try {
                Thread.sleep(CONCURRENT_LOOKUP_PAUSE_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw cause;
    }

    private static String resolveRequestId(String requestId) {
        if (requestId == null) {
            return UUID.randomUUID().toString();
        }
        if (!REQUEST_ID_FORMAT.matcher(requestId).matches()) {
            throw new BadRequestException("INVALID_IDEMPOTENCY_KEY",
                    "Idempotency key must be 1-64 characters: letters, digits, '.', '_', ':', '-'");
        }
        return requestId;
    }
}
