package ru.hotelbooking.hotel.hotel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import ru.hotelbooking.hotel.hotel.dto.HotelFilter;

final class HotelSpecifications {

    private HotelSpecifications() {
    }

    static Specification<Hotel> matching(HotelFilter filter) {
        List<Specification<Hotel>> specs = new ArrayList<>();
        if (filter.city() != null && !filter.city().isBlank()) {
            specs.add(cityIs(filter.city()));
        }
        if (filter.name() != null && !filter.name().isBlank()) {
            specs.add(nameContains(filter.name()));
        }
        if (filter.minStars() != null) {
            specs.add(starsAtLeast(filter.minStars()));
        }
        return Specification.allOf(specs);
    }

    private static Specification<Hotel> cityIs(String city) {
        String normalized = city.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.<String>get("city")), normalized);
    }

    private static Specification<Hotel> nameContains(String name) {
        String pattern = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.<String>get("name")), pattern);
    }

    private static Specification<Hotel> starsAtLeast(int stars) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Integer>get("stars"), stars);
    }
}
