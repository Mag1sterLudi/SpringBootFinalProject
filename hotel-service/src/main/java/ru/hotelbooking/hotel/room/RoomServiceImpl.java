package ru.hotelbooking.hotel.room;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.common.error.ConflictException;
import ru.hotelbooking.common.error.NotFoundException;
import ru.hotelbooking.common.validation.DateRanges;
import ru.hotelbooking.common.web.PageResponse;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.hotel.HotelRepository;
import ru.hotelbooking.hotel.reservation.RoomReservationRepository;
import ru.hotelbooking.hotel.room.dto.RoomRequest;
import ru.hotelbooking.hotel.room.dto.RoomResponse;
import ru.hotelbooking.hotel.room.dto.RoomSearchCriteria;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    static final Sort RECOMMENDATION_ORDER = Sort.by(Sort.Order.asc("timesBooked"), Sort.Order.asc("id"));

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomReservationRepository reservationRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public RoomResponse create(RoomRequest request) {
        Hotel hotel = findHotel(request.hotelId());
        if (roomRepository.existsByHotelIdAndNumber(hotel.getId(), request.number())) {
            throw duplicate(request);
        }
        Room room = roomMapper.toEntity(request);
        room.setHotel(hotel);
        Room saved = roomRepository.save(room);
        log.info("Room created: id={}, hotelId={}, number={}", saved.getId(), hotel.getId(), saved.getNumber());
        return roomMapper.toResponse(saved);
    }

    @Override
    public RoomResponse getById(Long id) {
        return roomMapper.toResponse(findRoom(id));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = findRoom(id);
        Hotel hotel = findHotel(request.hotelId());
        if (roomRepository.existsByHotelIdAndNumberAndIdNot(hotel.getId(), request.number(), id)) {
            throw duplicate(request);
        }
        roomMapper.update(request, room);
        room.setHotel(hotel);
        log.info("Room updated: id={}", id);
        return roomMapper.toResponse(room);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = findRoom(id);
        if (reservationRepository.existsByRoomId(id)) {
            throw new ConflictException("ROOM_HAS_RESERVATIONS",
                    "Room " + id + " has reservation history; set available=false instead");
        }
        roomRepository.delete(room);
        log.info("Room deleted: id={}", id);
    }

    @Override
    public PageResponse<RoomResponse> findFree(RoomSearchCriteria criteria, Pageable pageable) {
        DateRanges.requireValidOptional(criteria.startDate(), criteria.endDate());
        return PageResponse.from(roomRepository.findAll(RoomSpecifications.matching(criteria), pageable)
                .map(roomMapper::toResponse));
    }

    @Override
    public List<RoomResponse> recommend(RoomSearchCriteria criteria, int limit) {
        DateRanges.requireValidOptional(criteria.startDate(), criteria.endDate());
        PageRequest page = PageRequest.of(0, limit, RECOMMENDATION_ORDER);
        return roomRepository.findAll(RoomSpecifications.matching(criteria), page).stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    private Room findRoom(Long id) {
        return roomRepository.findWithHotelById(id).orElseThrow(() -> NotFoundException.of("Room", id));
    }

    private Hotel findHotel(Long id) {
        return hotelRepository.findById(id).orElseThrow(() -> NotFoundException.of("Hotel", id));
    }

    private static ConflictException duplicate(RoomRequest request) {
        return new ConflictException("ROOM_ALREADY_EXISTS",
                "Room " + request.number() + " already exists in hotel " + request.hotelId());
    }
}
