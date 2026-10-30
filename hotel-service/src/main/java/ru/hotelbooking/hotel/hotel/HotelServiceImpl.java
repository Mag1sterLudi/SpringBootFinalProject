package ru.hotelbooking.hotel.hotel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.common.error.ConflictException;
import ru.hotelbooking.common.error.NotFoundException;
import ru.hotelbooking.common.web.PageResponse;
import ru.hotelbooking.hotel.hotel.dto.HotelFilter;
import ru.hotelbooking.hotel.hotel.dto.HotelRequest;
import ru.hotelbooking.hotel.hotel.dto.HotelResponse;
import ru.hotelbooking.hotel.reservation.RoomReservationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomReservationRepository reservationRepository;
    private final HotelMapper hotelMapper;

    @Override
    @Transactional
    public HotelResponse create(HotelRequest request) {
        if (hotelRepository.existsByNameIgnoreCaseAndCityIgnoreCase(request.name(), request.city())) {
            throw duplicate(request);
        }
        Hotel hotel = hotelRepository.save(hotelMapper.toEntity(request));
        log.info("Hotel created: id={}, name='{}', city='{}'", hotel.getId(), hotel.getName(), hotel.getCity());
        return hotelMapper.toResponse(hotel);
    }

    @Override
    public PageResponse<HotelResponse> search(HotelFilter filter, Pageable pageable) {
        return PageResponse.from(hotelRepository.findAll(HotelSpecifications.matching(filter), pageable)
                .map(hotelMapper::toResponse));
    }

    @Override
    public HotelResponse getById(Long id) {
        return hotelMapper.toResponse(findHotel(id));
    }

    @Override
    @Transactional
    public HotelResponse update(Long id, HotelRequest request) {
        Hotel hotel = findHotel(id);
        if (hotelRepository.existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(request.name(), request.city(), id)) {
            throw duplicate(request);
        }
        hotelMapper.update(request, hotel);
        log.info("Hotel updated: id={}", id);
        return hotelMapper.toResponse(hotel);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Hotel hotel = findHotel(id);
        if (reservationRepository.existsByRoomHotelId(id)) {
            throw new ConflictException("HOTEL_HAS_RESERVATIONS",
                    "Hotel " + id + " has reservation history; mark its rooms unavailable instead");
        }
        hotelRepository.delete(hotel);
        log.info("Hotel deleted: id={}", id);
    }

    private Hotel findHotel(Long id) {
        return hotelRepository.findById(id).orElseThrow(() -> NotFoundException.of("Hotel", id));
    }

    private static ConflictException duplicate(HotelRequest request) {
        return new ConflictException("HOTEL_ALREADY_EXISTS",
                "Hotel '" + request.name() + "' already exists in " + request.city());
    }
}
