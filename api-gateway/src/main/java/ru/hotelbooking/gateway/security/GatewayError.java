package ru.hotelbooking.gateway.security;

import java.time.Instant;

/**
 * Те же поля, что у ApiError в сервисах, чтобы формат ошибок был один.
 */
public record GatewayError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId) {
}
