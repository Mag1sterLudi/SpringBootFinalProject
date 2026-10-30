package ru.hotelbooking.booking.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hotelbooking.booking.security.CurrentUser;
import ru.hotelbooking.booking.user.dto.CreateUserRequest;
import ru.hotelbooking.booking.user.dto.UpdateUserRequest;
import ru.hotelbooking.booking.user.dto.UserResponse;
import ru.hotelbooking.common.web.PageResponse;

@Tag(name = "Users (admin)")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Список пользователей")
    public PageResponse<UserResponse> findAll(@ParameterObject @PageableDefault(size = 20, sort = "id")
                                              Pageable pageable) {
        return userService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Пользователь по идентификатору")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пользователя с указанной ролью")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Частично обновить пользователя")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить пользователя без бронирований")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        userService.delete(id, CurrentUser.from(jwt));
    }
}
