package ru.hotelbooking.booking.user;

import org.mapstruct.Mapper;
import ru.hotelbooking.booking.user.dto.UserResponse;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}
