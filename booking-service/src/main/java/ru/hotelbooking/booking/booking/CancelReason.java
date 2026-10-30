package ru.hotelbooking.booking.booking;

public enum CancelReason {
    // отель ответил 409 (или не подошёл ни один кандидат)
    ROOM_NOT_AVAILABLE,
    ROOM_NOT_FOUND,
    // тайм-аут, 5xx или открытый circuit breaker
    HOTEL_UNAVAILABLE,
    REJECTED_BY_HOTEL,
    CANCELLED_BY_USER,
    // сага не завершилась (например, сервис упал посреди шага), бронь закрыта фоновой задачей
    PROCESSING_TIMEOUT
}
