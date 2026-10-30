package ru.hotelbooking.booking.client;

import ru.hotelbooking.common.error.ServiceUnavailableException;

public class HotelServiceUnavailableException extends ServiceUnavailableException {

    private static final String CODE = "HOTEL_SERVICE_UNAVAILABLE";

    public HotelServiceUnavailableException(String message) {
        super(CODE, message);
    }

    public HotelServiceUnavailableException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
