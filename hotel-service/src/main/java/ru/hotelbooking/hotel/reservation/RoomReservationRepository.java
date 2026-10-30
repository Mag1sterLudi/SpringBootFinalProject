package ru.hotelbooking.hotel.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    Optional<RoomReservation> findByRequestId(String requestId);

    boolean existsByRoomId(Long roomId);

    boolean existsByRoomHotelId(Long hotelId);

    @Query("""
            select case when count(r) > 0 then true else false end
            from RoomReservation r
            where r.room.id = :roomId
              and r.status = ru.hotelbooking.hotel.reservation.ReservationStatus.HELD
              and r.startDate < :endDate
              and r.endDate > :startDate
            """)
    boolean existsActiveOverlap(@Param("roomId") Long roomId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("""
            select r from RoomReservation r
            join fetch r.room room
            where room.hotel.id = :hotelId
              and r.status = ru.hotelbooking.hotel.reservation.ReservationStatus.HELD
              and r.startDate < :periodEnd
              and r.endDate > :periodStart
            """)
    List<RoomReservation> findActiveInPeriod(@Param("hotelId") Long hotelId,
                                             @Param("periodStart") LocalDate periodStart,
                                             @Param("periodEnd") LocalDate periodEnd);
}
