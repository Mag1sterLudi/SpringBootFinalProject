package ru.hotelbooking.hotel.hotel;

import org.springframework.data.domain.Pageable;
import ru.hotelbooking.common.web.PageResponse;
import ru.hotelbooking.hotel.hotel.dto.HotelFilter;
import ru.hotelbooking.hotel.hotel.dto.HotelRequest;
import ru.hotelbooking.hotel.hotel.dto.HotelResponse;

public interface HotelService {

    HotelResponse create(HotelRequest request);

    PageResponse<HotelResponse> search(HotelFilter filter, Pageable pageable);

    HotelResponse getById(Long id);

    HotelResponse update(Long id, HotelRequest request);

    void delete(Long id);
}
