package ru.hotelbooking.hotel.reservation;

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
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.hotelbooking.hotel.room.Room;

/**
 * Блокировка номера с startDate по endDate. request_id уникальный - на нём держится идемпотентность.
 */
@Entity
@Table(name = "room_reservations",
        uniqueConstraints = @UniqueConstraint(name = "uk_reservations_request_id", columnNames = "request_id"),
        indexes = {
            @Index(name = "idx_reservations_room_period", columnList = "room_id, status, start_date, end_date"),
            @Index(name = "idx_reservations_booking", columnList = "booking_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    public static RoomReservation hold(Room room, SlotRequest slot, Instant now) {
        RoomReservation reservation = new RoomReservation();
        reservation.room = room;
        reservation.requestId = slot.requestId();
        reservation.bookingId = slot.bookingId();
        reservation.startDate = slot.startDate();
        reservation.endDate = slot.endDate();
        reservation.status = ReservationStatus.HELD;
        reservation.createdAt = now;
        return reservation;
    }

    public boolean isHeld() {
        return status == ReservationStatus.HELD;
    }

    public boolean belongsToRoom(Long roomId) {
        return room.getId().equals(roomId);
    }

    public void release(Instant now) {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = now;
    }
}
