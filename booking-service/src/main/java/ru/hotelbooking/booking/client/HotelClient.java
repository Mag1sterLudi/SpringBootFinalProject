package ru.hotelbooking.booking.client;

import java.util.List;
import ru.hotelbooking.booking.client.dto.HoldRequest;
import ru.hotelbooking.booking.client.dto.HoldResponse;
import ru.hotelbooking.booking.client.dto.ReleaseRequest;
import ru.hotelbooking.booking.client.dto.ReleaseResult;
import ru.hotelbooking.booking.client.dto.RoomQuery;
import ru.hotelbooking.booking.client.dto.RoomView;

/**
 * Вызовы hotel-service. Ошибки - 409 (RoomUnavailableException, не ретраится),
 * тайм-аут / 5xx после ретраев (HotelServiceUnavailableException),
 * открытый circuit breaker (CallNotPermittedException).
 */
public interface HotelClient {

    List<RoomView> recommendRooms(RoomQuery query);

    HoldResponse confirmAvailability(Long roomId, HoldRequest request);

    ReleaseResult release(Long roomId, ReleaseRequest request);
}
