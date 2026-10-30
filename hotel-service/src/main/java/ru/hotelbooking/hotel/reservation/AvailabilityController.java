package ru.hotelbooking.hotel.reservation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hotelbooking.hotel.reservation.dto.AvailabilityRequest;
import ru.hotelbooking.hotel.reservation.dto.ReleaseRequest;
import ru.hotelbooking.hotel.reservation.dto.ReleaseResponse;
import ru.hotelbooking.hotel.reservation.dto.ReservationResponse;

/**
 * Внутренние эндпойнты для Booking Service. Через Gateway не публикуются.
 */
@Tag(name = "Internal: availability", description = "Шаги саги бронирования, только для роли INTERNAL")
@PreAuthorize("hasRole('INTERNAL')")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping("/{id}/confirm-availability")
    @Operation(summary = "Подтвердить доступность и заблокировать слот на период")
    public ReservationResponse confirmAvailability(@PathVariable Long id,
                                                   @Valid @RequestBody AvailabilityRequest request) {
        return availabilityService.confirmAvailability(id, request);
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Компенсация: снять блокировку слота")
    public ReleaseResponse release(@PathVariable Long id, @Valid @RequestBody ReleaseRequest request) {
        return availabilityService.release(id, request);
    }
}
