package ru.hotelbooking.booking.booking;

import ru.hotelbooking.booking.booking.dto.BookingResponse;

/**
 * created = false, если это повторный запрос с тем же ключом и вернули уже существующую бронь.
 */
public record BookingResult(BookingResponse booking, boolean created) {

    public static BookingResult newlyCreated(BookingResponse booking) {
        return new BookingResult(booking, true);
    }

    public static BookingResult replayed(BookingResponse booking) {
        return new BookingResult(booking, false);
    }
}
