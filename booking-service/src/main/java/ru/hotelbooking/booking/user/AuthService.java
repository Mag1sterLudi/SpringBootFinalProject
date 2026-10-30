package ru.hotelbooking.booking.user;

import ru.hotelbooking.booking.user.dto.AuthResponse;
import ru.hotelbooking.booking.user.dto.LoginRequest;
import ru.hotelbooking.booking.user.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse authenticate(LoginRequest request);
}
