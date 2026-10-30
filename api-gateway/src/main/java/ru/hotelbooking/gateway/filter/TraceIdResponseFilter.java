package ru.hotelbooking.gateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import ru.hotelbooking.gateway.tracing.CurrentTrace;

/**
 * Отдаёт клиенту traceId в заголовке X-Trace-Id, чтобы по нему можно было найти запрос в логах
 * всех сервисов. Стоит до security, поэтому заголовок есть и у ответов 401/403.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class TraceIdResponseFilter implements WebFilter {

    public static final String HEADER = "X-Trace-Id";

    private final CurrentTrace currentTrace;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().getHeaders().set(HEADER, currentTrace.traceId(exchange));
        return chain.filter(exchange);
    }
}
