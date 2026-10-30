package ru.hotelbooking.hotel.statistics;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotelbooking.common.error.BadRequestException;
import ru.hotelbooking.common.error.NotFoundException;
import ru.hotelbooking.hotel.hotel.Hotel;
import ru.hotelbooking.hotel.hotel.HotelRepository;
import ru.hotelbooking.hotel.reservation.RoomReservationRepository;
import ru.hotelbooking.hotel.room.RoomRepository;
import ru.hotelbooking.hotel.statistics.dto.HotelOccupancy;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OccupancyStatisticsServiceImpl implements OccupancyStatisticsService {

    private static final int DEFAULT_PERIOD_DAYS = 30;
    private static final int MAX_PERIOD_DAYS = 366;

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;
    private final OccupancyCalculator calculator;

    @Override
    public HotelOccupancy forHotel(Long hotelId, LocalDate from, LocalDate to) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> NotFoundException.of("Hotel", hotelId));
        Period period = resolvePeriod(from, to);
        return calculate(hotel, period);
    }

    @Override
    public List<HotelOccupancy> forAllHotels(LocalDate from, LocalDate to) {
        Period period = resolvePeriod(from, to);
        return hotelRepository.findAll().stream()
                .map(hotel -> calculate(hotel, period).withoutRooms())
                .sorted(Comparator.comparingDouble(HotelOccupancy::occupancyRate).reversed()
                        .thenComparing(HotelOccupancy::hotelId))
                .toList();
    }

    private HotelOccupancy calculate(Hotel hotel, Period period) {
        return calculator.calculate(
                hotel,
                roomRepository.findByHotelIdOrderByIdAsc(hotel.getId()),
                reservationRepository.findActiveInPeriod(hotel.getId(), period.from(), period.to()),
                period.from(),
                period.to());
    }

    private Period resolvePeriod(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(DEFAULT_PERIOD_DAYS);
        if (!end.isAfter(start) || end.isAfter(start.plusDays(MAX_PERIOD_DAYS))) {
            throw new BadRequestException("INVALID_PERIOD",
                    "Period must be positive and not longer than " + MAX_PERIOD_DAYS + " days");
        }
        return new Period(start, end);
    }

    private record Period(LocalDate from, LocalDate to) {
    }
}
