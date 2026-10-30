package ru.hotelbooking.hotel.room;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.hotelbooking.hotel.room.dto.RoomRequest;
import ru.hotelbooking.hotel.room.dto.RoomResponse;

@Mapper
public interface RoomMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    RoomResponse toResponse(Room room);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "timesBooked", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "available", source = "available", defaultValue = "true")
    Room toEntity(RoomRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "timesBooked", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(RoomRequest request, @MappingTarget Room room);
}
