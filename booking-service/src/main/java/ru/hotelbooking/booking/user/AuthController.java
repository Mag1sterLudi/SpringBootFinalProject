package ru.hotelbooking.booking.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hotelbooking.booking.user.dto.AuthResponse;
import ru.hotelbooking.booking.user.dto.LoginRequest;
import ru.hotelbooking.booking.user.dto.RegisterRequest;

@Tag(name = "Auth")
@SecurityRequirements
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/user/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация пользователя (роль USER) с выдачей токена")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/user/auth")
    @Operation(summary = "Вход: обмен логина и пароля на JWT (срок действия 1 час)")
    public AuthResponse authenticate(@Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request);
    }
}
