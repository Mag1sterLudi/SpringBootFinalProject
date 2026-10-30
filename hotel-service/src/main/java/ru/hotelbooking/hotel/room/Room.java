package ru.hotelbooking.hotel.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.hotelbooking.hotel.hotel.Hotel;

@Entity
@Table(name = "rooms",
        uniqueConstraints = @UniqueConstraint(name = "uk_rooms_hotel_number", columnNames = {"hotel_id", "number"}),
        indexes = {
            @Index(name = "idx_rooms_hotel", columnList = "hotel_id"),
            @Index(name = "idx_rooms_times_booked", columnList = "times_booked, id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 10)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type = RoomType.STANDARD;

    @Column(nullable = false)
    private int capacity = 2;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    // false - номер на ремонте / выведен. Занятость по датам смотрим по room_reservations, не по этому полю
    @Column(nullable = false)
    private boolean available = true;

    // число активных броней номера, по нему сортируются рекомендации
    @Column(name = "times_booked", nullable = false)
    private long timesBooked;

    @Version
    private long version;

    public void registerBooking() {
        timesBooked++;
    }

    public void unregisterBooking() {
        if (timesBooked > 0) {
            timesBooked--;
        }
    }
}
