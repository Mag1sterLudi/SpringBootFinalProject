package ru.hotelbooking.hotel.room;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.reservation.ReservationStatus;
import ru.hotelbooking.hotel.reservation.RoomReservation;
import ru.hotelbooking.hotel.room.dto.RoomSearchCriteria;

final class RoomSpecifications {

    private RoomSpecifications() {
    }

    static Specification<Room> matching(RoomSearchCriteria criteria) {
        List<Specification<Room>> specs = new ArrayList<>();
        specs.add(inService());
        if (criteria.hotelId() != null) {
            specs.add(hotelIs(criteria.hotelId()));
        }
        if (criteria.city() != null && !criteria.city().isBlank()) {
            specs.add(cityIs(criteria.city()));
        }
        if (criteria.type() != null) {
            specs.add(typeIs(criteria.type()));
        }
        if (criteria.minCapacity() != null) {
            specs.add(capacityAtLeast(criteria.minCapacity()));
        }
        if (criteria.maxPrice() != null) {
            specs.add(priceAtMost(criteria.maxPrice()));
        }
        if (criteria.hasPeriod()) {
            specs.add(freeBetween(criteria.startDate(), criteria.endDate()));
        }
        return Specification.allOf(specs);
    }

    private static Specification<Room> inService() {
        return (root, query, cb) -> cb.isTrue(root.<Boolean>get("available"));
    }

    private static Specification<Room> hotelIs(Long hotelId) {
        return (root, query, cb) -> cb.equal(root.<Hotel>get("hotel").<Long>get("id"), hotelId);
    }

    private static Specification<Room> cityIs(String city) {
        String normalized = city.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.<Hotel>get("hotel").<String>get("city")), normalized);
    }

    private static Specification<Room> typeIs(RoomType type) {
        return (root, query, cb) -> cb.equal(root.<RoomType>get("type"), type);
    }

    private static Specification<Room> capacityAtLeast(int capacity) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Integer>get("capacity"), capacity);
    }

    private static Specification<Room> priceAtMost(BigDecimal price) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("pricePerNight"), price);
    }

    // нет HELD-блокировки, пересекающей период с start по end
    private static Specification<Room> freeBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            Subquery<Long> overlapping = query.subquery(Long.class);
            Root<RoomReservation> reservation = overlapping.from(RoomReservation.class);
            overlapping.select(reservation.<Long>get("id")).where(
                    cb.equal(reservation.<Room>get("room"), root),
                    cb.equal(reservation.<ReservationStatus>get("status"), ReservationStatus.HELD),
                    cb.lessThan(reservation.<LocalDate>get("startDate"), end),
                    cb.greaterThan(reservation.<LocalDate>get("endDate"), start));
            return cb.not(cb.exists(overlapping));
        };
    }
}
