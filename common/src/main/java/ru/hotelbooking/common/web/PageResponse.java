package ru.hotelbooking.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Страница для ответа API. PageImpl напрямую не отдаю - Spring Data предупреждает,
 * что его JSON-формат не гарантируется.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
