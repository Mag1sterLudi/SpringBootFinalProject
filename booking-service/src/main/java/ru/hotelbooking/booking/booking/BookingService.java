package ru.hotelbooking.booking.booking;

import org.springframework.data.domain.Pageable;
import ru.hotelbooking.booking.booking.dto.BookingResponse;
import ru.hotelbooking.booking.booking.dto.CreateBookingRequest;
import ru.hotelbooking.booking.security.CurrentUser;
import ru.hotelbooking.common.web.PageResponse;

public interface BookingService {

    BookingResult create(CurrentUser user, CreateBookingRequest request);

    PageResponse<BookingResponse> history(CurrentUser user, BookingStatus status, Pageable pageable);

    BookingResponse get(CurrentUser user, Long bookingId);

    BookingResponse cancel(CurrentUser user, Long bookingId);
}
