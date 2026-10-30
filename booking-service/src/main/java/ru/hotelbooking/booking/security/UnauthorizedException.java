package ru.hotelbooking.booking.security;

import org.springframework.http.HttpStatus;
import ru.hotelbooking.common.error.ApiException;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
