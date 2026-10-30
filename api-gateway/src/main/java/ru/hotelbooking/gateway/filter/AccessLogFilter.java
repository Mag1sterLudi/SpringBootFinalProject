package ru.hotelbooking.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Access-лог - метод, путь, статус, время. Стоит раньше security, поэтому 401/403 тоже попадают в лог.
 * traceId добавляет Micrometer Tracing и передаёт его дальше в заголовке traceparent.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startedAt = System.nanoTime();
        return chain.filter(exchange)
                .doFinally(signal -> log.info("method={} path={} status={} durationMs={}",
                        request.getMethod(),
                        request.getPath().value(),
                        exchange.getResponse().getStatusCode(),
                        (System.nanoTime() - startedAt) / 1_000_000));
    }
}
