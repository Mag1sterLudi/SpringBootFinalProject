package ru.hotelbooking.hotel.reservation.dto;

public record ReleaseResponse(String requestId, Long roomId, ReleaseOutcome outcome) {

    public enum ReleaseOutcome {
        RELEASED,
        ALREADY_RELEASED,
        NOTHING_TO_RELEASE
    }
}
