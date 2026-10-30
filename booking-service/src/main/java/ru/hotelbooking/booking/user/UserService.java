package ru.hotelbooking.booking.user;

import org.springframework.data.domain.Pageable;
import ru.hotelbooking.booking.security.CurrentUser;
import ru.hotelbooking.booking.user.dto.CreateUserRequest;
import ru.hotelbooking.booking.user.dto.UpdateUserRequest;
import ru.hotelbooking.booking.user.dto.UserResponse;
import ru.hotelbooking.common.web.PageResponse;

public interface UserService {

    PageResponse<UserResponse> findAll(Pageable pageable);

    UserResponse getById(Long id);

    UserResponse create(CreateUserRequest request);

    UserResponse update(Long id, UpdateUserRequest request);

    void delete(Long id, CurrentUser initiator);
}
