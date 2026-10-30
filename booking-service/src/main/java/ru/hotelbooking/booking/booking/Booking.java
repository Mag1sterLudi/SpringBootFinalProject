package ru.hotelbooking.booking.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.hotelbooking.booking.user.User;
import ru.hotelbooking.common.validation.DateRange;
import ru.hotelbooking.common.validation.DateRanges;

/**
 * Бронь и её статусы. PENDING -> CONFIRMED или CANCELLED, CONFIRMED -> CANCELLED.
 * Статус меняется только через confirm()/cancel().
 */
@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uk_bookings_request_id", columnNames = "request_id"),
        indexes = {
            @Index(name = "idx_bookings_user_created", columnList = "user_id, created_at"),
            @Index(name = "idx_bookings_room_period", columnList = "room_id, status, start_date, end_date"),
            @Index(name = "idx_bookings_status_created", columnList = "status, created_at"),
            @Index(name = "idx_bookings_release_pending", columnList = "release_pending, updated_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // тот же user_id, но read-only: чтобы проверить владельца, не загружая User
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BookingStatus status;

    // ключ повторного запроса, в hotel-service уходит как requestId
    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 32)
    private CancelReason cancelReason;

    // бронь отменена, а release в отеле ещё не прошёл; добивает SagaRecoveryJob
    @Column(name = "release_pending", nullable = false)
    private boolean releasePending;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public static Booking pending(User user, Long roomId, Long hotelId, DateRange period, String requestId,
                                  Instant now) {
        Booking booking = new Booking();
        booking.user = user;
        booking.userId = user.getId();
        booking.roomId = roomId;
        booking.hotelId = hotelId;
        booking.startDate = period.startDate();
        booking.endDate = period.endDate();
        booking.requestId = requestId;
        booking.status = BookingStatus.PENDING;
        booking.createdAt = now;
        booking.updatedAt = now;
        return booking;
    }

    public void assignRoom(Long newRoomId, Instant now) {
        requireStatus(BookingStatus.PENDING);
        this.roomId = newRoomId;
        this.hotelId = null;
        this.updatedAt = now;
    }

    public void confirm(Long confirmedHotelId, Instant now) {
        requireStatus(BookingStatus.PENDING);
        this.status = BookingStatus.CONFIRMED;
        if (confirmedHotelId != null) {
            this.hotelId = confirmedHotelId;
        }
        this.updatedAt = now;
    }

    // false, если бронь уже отменена (повторная отмена - не ошибка)
    public boolean cancel(CancelReason reason, Instant now) {
        if (status == BookingStatus.CANCELLED) {
            return false;
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelReason = reason;
        this.updatedAt = now;
        return true;
    }

    public void awaitRelease(Instant now) {
        this.releasePending = true;
        this.updatedAt = now;
    }

    public void releaseConfirmed(Instant now) {
        this.releasePending = false;
        this.updatedAt = now;
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public long getNights() {
        return DateRanges.nights(startDate, endDate);
    }

    private void requireStatus(BookingStatus expected) {
        if (status != expected) {
            throw new BookingStateException("Booking " + id + " is " + status + ", expected " + expected);
        }
    }
}
