package ru.hotelbooking.hotel.hotel;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.hotelbooking.hotel.hotel.dto.HotelRequest;
import ru.hotelbooking.hotel.hotel.dto.HotelResponse;

@Mapper
public interface HotelMapper {

    HotelResponse toResponse(Hotel hotel);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Hotel toEntity(HotelRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(HotelRequest request, @MappingTarget Hotel hotel);
}
