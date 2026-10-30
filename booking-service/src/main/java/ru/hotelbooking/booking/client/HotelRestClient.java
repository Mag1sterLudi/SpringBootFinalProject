package ru.hotelbooking.booking.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.hotelbooking.booking.client.dto.HoldRequest;
import ru.hotelbooking.booking.client.dto.HoldResponse;
import ru.hotelbooking.booking.client.dto.ReleaseRequest;
import ru.hotelbooking.booking.client.dto.ReleaseResult;
import ru.hotelbooking.booking.client.dto.RoomQuery;
import ru.hotelbooking.booking.client.dto.RoomView;
import ru.hotelbooking.common.error.NotFoundException;

/**
 * HTTP-реализация HotelClient. Порядок аспектов Resilience4j по умолчанию -
 * Retry(CircuitBreaker(call)), т.е. каждая попытка учитывается circuit breakerом,
 * а при разомкнутой цепи повтор не выполняется.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelRestClient implements HotelClient {

    static final String RESILIENCE_INSTANCE = "hotel";

    private static final ParameterizedTypeReference<List<RoomView>> ROOM_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient hotelRestClient;
    private final ObjectMapper objectMapper;

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public List<RoomView> recommendRooms(RoomQuery query) {
        return call("recommend rooms", () -> hotelRestClient.get()
                .uri(uri -> uri.path("/api/rooms/recommend")
                        .queryParamIfPresent("hotelId", Optional.ofNullable(query.hotelId()))
                        .queryParam("startDate", query.startDate())
                        .queryParam("endDate", query.endDate())
                        .queryParam("limit", query.limit())
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translateError)
                .body(ROOM_LIST));
    }

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public HoldResponse confirmAvailability(Long roomId, HoldRequest request) {
        log.info("bookingId={} -> hotel confirm-availability: roomId={}, requestId={}",
                request.bookingId(), roomId, request.requestId());
        return call("confirm availability", () -> hotelRestClient.post()
                .uri("/api/rooms/{id}/confirm-availability", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translateError)
                .body(HoldResponse.class));
    }

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public ReleaseResult release(Long roomId, ReleaseRequest request) {
        log.info("bookingId={} -> hotel release: roomId={}, requestId={}",
                request.bookingId(), roomId, request.requestId());
        return call("release", () -> hotelRestClient.post()
                .uri("/api/rooms/{id}/release", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translateError)
                .body(ReleaseResult.class));
    }

    private <T> T call(String operation, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException | IllegalStateException ex) {
            // IllegalStateException бросает балансировщик, если в Eureka нет ни одного экземпляра hotel-service
            log.warn("Hotel call '{}' failed: {}", operation, ex.getMessage());
            throw new HotelServiceUnavailableException("Hotel service call failed: " + operation, ex);
        }
    }

    private void translateError(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        String message = extractMessage(response);
        if (status.isSameCodeAs(HttpStatus.CONFLICT)) {
            throw new RoomUnavailableException(message);
        }
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            throw new NotFoundException(message);
        }
        log.warn("Hotel responded {} on {} {}: {}", status.value(), request.getMethod(), request.getURI(), message);
        throw new HotelServiceUnavailableException("Hotel service responded with " + status.value());
    }

    private String extractMessage(ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            JsonNode message = objectMapper.readTree(body).path("message");
            return message.isTextual() ? message.asText() : body;
        } catch (IOException ex) {
            return body;
        }
    }
}
