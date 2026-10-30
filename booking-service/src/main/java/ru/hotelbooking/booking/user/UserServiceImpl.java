package ru.hotelbooking.booking.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.booking.booking.BookingRepository;
import ru.hotelbooking.booking.security.CurrentUser;
import ru.hotelbooking.booking.user.dto.CreateUserRequest;
import ru.hotelbooking.booking.user.dto.UpdateUserRequest;
import ru.hotelbooking.booking.user.dto.UserResponse;
import ru.hotelbooking.common.error.ConflictException;
import ru.hotelbooking.common.error.NotFoundException;
import ru.hotelbooking.common.web.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Override
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw usernameTaken(request.username());
        }
        User user = userRepository.save(
                new User(request.username(), passwordEncoder.encode(request.password()), request.role()));
        log.info("User created by admin: id={}, username={}, role={}",
                user.getId(), user.getUsername(), user.getRole());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        if (request.username() != null) {
            if (userRepository.existsByUsernameAndIdNot(request.username(), id)) {
                throw usernameTaken(request.username());
            }
            user.setUsername(request.username());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        log.info("User updated by admin: id={}", id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long id, CurrentUser initiator) {
        if (id.equals(initiator.id())) {
            throw new ConflictException("CANNOT_DELETE_SELF", "Administrator cannot delete own account");
        }
        User user = findUser(id);
        if (bookingRepository.existsByUserId(id)) {
            throw new ConflictException("USER_HAS_BOOKINGS", "User " + id + " has bookings and cannot be deleted");
        }
        userRepository.delete(user);
        log.info("User deleted by admin: id={}", id);
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }

    private static ConflictException usernameTaken(String username) {
        return new ConflictException("USERNAME_TAKEN", "Username '" + username + "' is already taken");
    }
}
