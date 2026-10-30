package ru.hotelbooking.booking.client;

import ru.hotelbooking.common.error.ConflictException;

public class RoomUnavailableException extends ConflictException {

    public RoomUnavailableException(String message) {
        super("ROOM_NOT_AVAILABLE", message);
    }
}
