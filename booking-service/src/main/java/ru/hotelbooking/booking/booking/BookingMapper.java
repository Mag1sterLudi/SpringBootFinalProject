package ru.hotelbooking.booking.booking;

import org.mapstruct.Mapper;
import ru.hotelbooking.booking.booking.dto.BookingResponse;

@Mapper
public interface BookingMapper {

    BookingResponse toResponse(Booking booking);
}
