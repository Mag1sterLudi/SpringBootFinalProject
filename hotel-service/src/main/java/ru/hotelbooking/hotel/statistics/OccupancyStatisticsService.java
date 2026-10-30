package ru.hotelbooking.hotel.statistics;

import java.time.LocalDate;
import java.util.List;
import ru.hotelbooking.hotel.statistics.dto.HotelOccupancy;

public interface OccupancyStatisticsService {

    HotelOccupancy forHotel(Long hotelId, LocalDate from, LocalDate to);

    // все отели, сначала самые загруженные
    List<HotelOccupancy> forAllHotels(LocalDate from, LocalDate to);
}
