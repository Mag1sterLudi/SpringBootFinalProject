package ru.hotelbooking.booking.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.hotelbooking.common.error.ApiError;
import ru.hotelbooking.common.error.ApiErrors;

// CallNotPermittedException не наследует ApiException, поэтому отдельный обработчик
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HotelIntegrationExceptionHandler {

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiError> handleOpenCircuit(CallNotPermittedException ex, HttpServletRequest request) {
        log.warn("Hotel circuit breaker is open: {}", ex.getMessage());
        ApiError body = ApiErrors.of(HttpStatus.SERVICE_UNAVAILABLE, "HOTEL_CIRCUIT_OPEN",
                "Hotel service is temporarily unavailable, try again later", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
