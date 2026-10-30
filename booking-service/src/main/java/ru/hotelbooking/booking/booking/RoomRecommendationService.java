package ru.hotelbooking.booking.booking;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hotelbooking.booking.client.HotelClient;
import ru.hotelbooking.booking.client.HotelClientProperties;
import ru.hotelbooking.booking.client.dto.RoomQuery;
import ru.hotelbooking.booking.client.dto.RoomView;

/**
 * Подбор номера. Первым идёт свободный номер с наименьшим times_booked, при равенстве - с меньшим id.
 * Так брони распределяются по номерам равномерно и редко используемые номера не простаивают.
 * hotel-service отдаёт список уже в этом порядке, но сортирую ещё раз, чтобы не зависеть от него.
 */
@Service
@RequiredArgsConstructor
public class RoomRecommendationService {

    static final Comparator<RoomView> LEAST_BOOKED_FIRST =
            Comparator.comparingLong(RoomView::timesBooked).thenComparing(RoomView::id);

    private final HotelClient hotelClient;
    private final HotelClientProperties properties;

    public List<RoomView> recommend(Long hotelId, LocalDate startDate, LocalDate endDate) {
        RoomQuery query = new RoomQuery(hotelId, startDate, endDate, properties.maxCandidates());
        return hotelClient.recommendRooms(query).stream()
                .filter(RoomView::available)
                .sorted(LEAST_BOOKED_FIRST)
                .toList();
    }
}
