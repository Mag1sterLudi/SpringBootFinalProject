package ru.hotelbooking.booking.client.dto;

public record HoldResponse(Long reservationId, Long roomId, Long hotelId, String requestId, String status) {
}
