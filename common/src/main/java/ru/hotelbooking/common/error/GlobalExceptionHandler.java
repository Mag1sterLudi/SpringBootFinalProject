package ru.hotelbooking.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.hotelbooking.common.error.ApiError.Violation;

/**
 * Централизованное преобразование исключений в ApiError.
 * Стоит последним по приоритету, чтобы сервисы могли добавить свои обработчики.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.warn("{} {} failed: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        } else {
            log.debug("{} {} rejected: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
        return respond(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBindingErrors(BindException ex, HttpServletRequest request) {
        List<Violation> violations = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new Violation(fieldName(error), error.getDefaultMessage()))
                .toList();
        return validationFailed(violations, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex,
                                                              HttpServletRequest request) {
        List<Violation> violations = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new Violation(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return validationFailed(violations, request);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            PropertyReferenceException.class,
            InvalidDataAccessApiUsageException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed",
                request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        log.info("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "DATA_CONFLICT", "Request conflicts with the current state of data",
                request, List.of());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleConcurrentModification(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Resource was modified concurrently, retry the request", request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Endpoint not found", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            // стандартные ошибки Spring MVC (405, 415, 406 и т.п.) уже знают свой статус
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return respond(status, status.name(), ex.getMessage(), request, List.of());
        }
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error",
                request, List.of());
    }

    private ResponseEntity<ApiError> validationFailed(List<Violation> violations, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, violations);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String code, String message,
                                             HttpServletRequest request, List<Violation> violations) {
        ApiError body = ApiErrors.of(status, code, message, request.getRequestURI(), violations);
        return ResponseEntity.status(status).body(body);
    }

    private static String fieldName(ObjectError error) {
        return error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
    }
}
