package ru.hotelbooking.gateway.config;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import ru.hotelbooking.gateway.tracing.CurrentTrace;

/**
 * Ошибки шлюза (404 - нет маршрута, 503 - сервис не найден в Eureka) приводятся
 * к формату ApiError из сервисов - code, message и traceId вместо requestId.
 */
@Component
@RequiredArgsConstructor
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    private final CurrentTrace currentTrace;

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(request, options);
        HttpStatus status = attributes.get("status") instanceof Integer code ? HttpStatus.resolve(code) : null;
        if (status != null) {
            attributes.put("code", status.name());
            attributes.putIfAbsent("message", defaultMessage(status));
        }
        attributes.remove("requestId");
        attributes.put("traceId", currentTrace.traceId(request.exchange()));
        return attributes;
    }

    private static String defaultMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "No route for this path";
            case SERVICE_UNAVAILABLE -> "Service is temporarily unavailable, try again later";
            default -> status.getReasonPhrase();
        };
    }
}
