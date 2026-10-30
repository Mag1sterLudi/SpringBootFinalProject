package ru.hotelbooking.hotel.reservation;

import ru.hotelbooking.hotel.reservation.dto.AvailabilityRequest;
import ru.hotelbooking.hotel.reservation.dto.ReleaseRequest;
import ru.hotelbooking.hotel.reservation.dto.ReleaseResponse;
import ru.hotelbooking.hotel.reservation.dto.ReservationResponse;

/**
 * Блокировка слота и её снятие (шаги саги). Обе операции идемпотентны по requestId.
 */
public interface AvailabilityService {

    ReservationResponse confirmAvailability(Long roomId, AvailabilityRequest request);

    ReleaseResponse release(Long roomId, ReleaseRequest request);
}
