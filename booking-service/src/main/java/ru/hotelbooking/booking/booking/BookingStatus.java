package ru.hotelbooking.booking.booking;

import java.util.List;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    /** Статусы, при которых бронирование занимает номер. */
    public static final List<BookingStatus> ACTIVE = List.of(PENDING, CONFIRMED);
}
