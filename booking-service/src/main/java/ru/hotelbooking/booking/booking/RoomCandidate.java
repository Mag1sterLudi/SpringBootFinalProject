package ru.hotelbooking.booking.booking;

import ru.hotelbooking.booking.client.dto.RoomView;

/**
 * Номер, который сага пробует занять. При ручном выборе hotelId может быть null.
 */
public record RoomCandidate(Long roomId, Long hotelId) {

    public static RoomCandidate of(RoomView room) {
        return new RoomCandidate(room.id(), room.hotelId());
    }
}
