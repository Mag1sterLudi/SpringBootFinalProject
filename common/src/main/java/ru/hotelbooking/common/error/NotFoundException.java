package ru.hotelbooking.common.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException(entity + " with id " + id + " not found");
    }
}
