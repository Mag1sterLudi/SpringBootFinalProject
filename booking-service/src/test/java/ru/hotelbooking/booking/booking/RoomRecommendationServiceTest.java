package ru.hotelbooking.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hotelbooking.booking.client.HotelClient;
import ru.hotelbooking.booking.client.HotelClientProperties;
import ru.hotelbooking.booking.client.dto.RoomQuery;
import ru.hotelbooking.booking.client.dto.RoomView;

@ExtendWith(MockitoExtension.class)
class RoomRecommendationServiceTest {

    private static final LocalDate START = LocalDate.of(2030, 5, 1);

    @Mock
    private HotelClient hotelClient;

    @Test
    @DisplayName("Номера сортируются по возрастанию times_booked, при равенстве - по id; недоступные отбрасываются")
    void sortsByTimesBookedThenId() {
        // given
        when(hotelClient.recommendRooms(any(RoomQuery.class))).thenReturn(List.of(
                room(4L, 3, true),
                room(8L, 0, true),
                room(2L, 3, true),
                room(5L, 0, false),
                room(6L, 1, true)));
        RoomRecommendationService service = new RoomRecommendationService(hotelClient, properties());

        // when
        List<RoomView> result = service.recommend(1L, START, START.plusDays(2));

        // then
        assertThat(result).extracting(RoomView::id).containsExactly(8L, 6L, 2L, 4L);
    }

    private static HotelClientProperties properties() {
        return new HotelClientProperties("http://hotel-service", null, null, 5);
    }

    private static RoomView room(Long id, long timesBooked, boolean available) {
        return new RoomView(id, 1L, "Волга", String.valueOf(id), "STANDARD", 2, null, available, timesBooked);
    }
}
