package ru.hotelbooking.hotel.room;

import java.util.List;
import org.springframework.data.domain.Pageable;
import ru.hotelbooking.common.web.PageResponse;
import ru.hotelbooking.hotel.room.dto.RoomRequest;
import ru.hotelbooking.hotel.room.dto.RoomResponse;
import ru.hotelbooking.hotel.room.dto.RoomSearchCriteria;

public interface RoomService {

    RoomResponse create(RoomRequest request);

    RoomResponse getById(Long id);

    RoomResponse update(Long id, RoomRequest request);

    void delete(Long id);

    PageResponse<RoomResponse> findFree(RoomSearchCriteria criteria, Pageable pageable);

    // свободные номера по times_booked, потом по id
    List<RoomResponse> recommend(RoomSearchCriteria criteria, int limit);
}
