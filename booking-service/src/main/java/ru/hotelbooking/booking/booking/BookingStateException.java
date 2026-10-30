package ru.hotelbooking.booking.booking;

import ru.hotelbooking.common.error.ConflictException;

public class BookingStateException extends ConflictException {

    public BookingStateException(String message) {
        super("INVALID_BOOKING_STATE", message);
    }
}
