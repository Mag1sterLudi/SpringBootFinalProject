package ru.hotelbooking.hotel.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    boolean existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(String name, String city, Long id);
}
