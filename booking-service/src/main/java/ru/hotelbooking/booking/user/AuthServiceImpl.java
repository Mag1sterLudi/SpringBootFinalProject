package ru.hotelbooking.booking.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.booking.security.TokenService;
import ru.hotelbooking.booking.security.UnauthorizedException;
import ru.hotelbooking.booking.user.dto.AuthResponse;
import ru.hotelbooking.booking.user.dto.LoginRequest;
import ru.hotelbooking.booking.user.dto.RegisterRequest;
import ru.hotelbooking.common.error.ConflictException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("USERNAME_TAKEN", "Username '" + request.username() + "' is already taken");
        }
        User user = userRepository.save(
                new User(request.username(), passwordEncoder.encode(request.password()), Role.USER));
        log.info("User registered: id={}, username={}", user.getId(), user.getUsername());
        return tokenService.issueFor(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> {
                    log.info("Failed login attempt: username={}", request.username());
                    return new UnauthorizedException("BAD_CREDENTIALS", "Invalid username or password");
                });
        log.info("User authenticated: id={}, username={}", user.getId(), user.getUsername());
        return tokenService.issueFor(user);
    }
}
