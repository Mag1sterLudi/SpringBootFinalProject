package ru.hotelbooking.hotel.reservation;

import ru.hotelbooking.common.validation.DateRange;

// общее у запросов confirm-availability и release
public interface SlotRequest extends DateRange {

    String requestId();

    Long bookingId();
}
