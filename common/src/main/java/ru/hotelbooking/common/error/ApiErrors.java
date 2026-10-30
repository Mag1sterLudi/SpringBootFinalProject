package ru.hotelbooking.common.error;

import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

public final class ApiErrors {

    private static final String TRACE_ID_KEY = "traceId";

    private ApiErrors() {
    }

    public static ApiError of(HttpStatus status, String code, String message, String path) {
        return of(status, code, message, path, List.of());
    }

    public static ApiError of(HttpStatus status, String code, String message, String path,
                              List<ApiError.Violation> violations) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                MDC.get(TRACE_ID_KEY),
                violations);
    }
}
