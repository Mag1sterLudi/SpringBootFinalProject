package ru.hotelbooking.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Тело ответа с ошибкой, одинаковое во всех сервисах.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId,
        List<Violation> violations) {

    public record Violation(String field, String message) {
    }
}
