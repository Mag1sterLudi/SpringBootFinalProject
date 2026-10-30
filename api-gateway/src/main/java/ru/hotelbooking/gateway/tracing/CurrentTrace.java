package ru.hotelbooking.gateway.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
@RequiredArgsConstructor
public class CurrentTrace {

    private final Tracer tracer;

    // если спана почему-то нет, хотя бы id запроса внутри шлюза
    public String traceId(ServerWebExchange exchange) {
        Span span = tracer.currentSpan();
        return span != null ? span.context().traceId() : exchange.getRequest().getId();
    }
}
