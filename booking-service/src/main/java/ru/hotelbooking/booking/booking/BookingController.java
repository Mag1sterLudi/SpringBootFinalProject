package ru.hotelbooking.booking.booking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hotelbooking.booking.booking.dto.BookingResponse;
import ru.hotelbooking.booking.booking.dto.CreateBookingRequest;
import ru.hotelbooking.booking.booking.dto.RecommendationQuery;
import ru.hotelbooking.booking.client.dto.RoomView;
import ru.hotelbooking.booking.security.CurrentUser;
import ru.hotelbooking.common.web.PageResponse;

@Tag(name = "Bookings")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final RoomRecommendationService recommendationService;

    @PostMapping("/booking")
    @Operation(summary = "Создать бронирование (ручной выбор номера или autoSelect)",
            description = "201 - создано новое бронирование; 200 - повторная доставка запроса с тем же ключом, "
                    + "возвращается уже существующее бронирование")
    public ResponseEntity<BookingResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResult result = bookingService.create(CurrentUser.from(jwt), request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.booking());
    }

    @GetMapping("/bookings")
    @Operation(summary = "История бронирований текущего пользователя")
    public PageResponse<BookingResponse> history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) BookingStatus status,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return bookingService.history(CurrentUser.from(jwt), status, pageable);
    }

    @GetMapping("/booking/recommendations")
    @Operation(summary = "Свободные номера в порядке рекомендации (times_booked по возрастанию, затем id)")
    public List<RoomView> recommendations(@Valid @ParameterObject RecommendationQuery query) {
        return recommendationService.recommend(query.hotelId(), query.startDate(), query.endDate());
    }

    @GetMapping("/booking/{id}")
    @Operation(summary = "Бронирование по id (только своё; ADMIN - любое)")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.get(CurrentUser.from(jwt), id);
    }

    @DeleteMapping("/booking/{id}")
    @Operation(summary = "Отменить бронирование и освободить номер в отеле")
    public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.cancel(CurrentUser.from(jwt), id);
    }
}
