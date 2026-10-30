package ru.hotelbooking.booking.seed;

import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.booking.booking.Booking;
import ru.hotelbooking.booking.booking.BookingRepository;
import ru.hotelbooking.booking.booking.CancelReason;
import ru.hotelbooking.booking.user.Role;
import ru.hotelbooking.booking.user.User;
import ru.hotelbooking.booking.user.UserRepository;
import ru.hotelbooking.common.validation.DateRange;

/**
 * Демо-данные. admin/admin123, ivan/ivan123, maria/maria123.
 * Брони 1-4 совпадают с блокировками из DemoDataLoader в hotel-service (те же roomId и requestId).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(new User("admin", passwordEncoder.encode("admin123"), Role.ADMIN));
        User ivan = userRepository.save(new User("ivan", passwordEncoder.encode("ivan123"), Role.USER));
        User maria = userRepository.save(new User("maria", passwordEncoder.encode("maria123"), Role.USER));

        LocalDate today = LocalDate.now();
        confirmed(ivan, 1L, 1L, today.plusDays(5), today.plusDays(8), "seed-booking-1");
        confirmed(ivan, 6L, 2L, today.plusDays(10), today.plusDays(12), "seed-booking-2");
        confirmed(maria, 9L, 3L, today.plusDays(3), today.plusDays(7), "seed-booking-3");
        confirmed(maria, 2L, 1L, today.minusDays(20), today.minusDays(17), "seed-booking-4");

        Booking rejected = Booking.pending(ivan, 3L, 1L, period(today.plusDays(1), today.plusDays(2)),
                "seed-booking-5", Instant.now());
        rejected.cancel(CancelReason.ROOM_NOT_AVAILABLE, Instant.now());
        bookingRepository.save(rejected);

        log.info("Demo data loaded: {} users, {} bookings", userRepository.count(), bookingRepository.count());
    }

    private void confirmed(User user, Long roomId, Long hotelId, LocalDate start, LocalDate end, String requestId) {
        Instant now = Instant.now();
        Booking booking = Booking.pending(user, roomId, hotelId, period(start, end), requestId, now);
        booking.confirm(hotelId, now);
        bookingRepository.save(booking);
    }

    private static DateRange period(LocalDate start, LocalDate end) {
        return new Stay(start, end);
    }

    private record Stay(LocalDate startDate, LocalDate endDate) implements DateRange {
    }
}
