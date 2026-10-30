package ru.hotelbooking.booking.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByRequestId(String requestId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    boolean existsByUserId(Long userId);

    List<Booking> findByReleasePendingTrueOrderByUpdatedAtAsc(Pageable pageable);

    List<Booking> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(BookingStatus status, Instant createdBefore,
                                                                    Pageable pageable);

    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.roomId = :roomId
              and b.status in :statuses
              and b.startDate < :endDate
              and b.endDate > :startDate
            """)
    boolean existsOverlapping(@Param("roomId") Long roomId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("statuses") Collection<BookingStatus> statuses);
}
