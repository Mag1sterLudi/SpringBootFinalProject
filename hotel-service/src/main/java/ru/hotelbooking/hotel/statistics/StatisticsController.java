package ru.hotelbooking.hotel.statistics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hotelbooking.hotel.statistics.dto.HotelOccupancy;

@Tag(name = "Statistics", description = "Загруженность номеров (ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class StatisticsController {

    private final OccupancyStatisticsService statisticsService;

    @GetMapping("/statistics")
    @Operation(summary = "Сводная загруженность всех отелей за период (по умолчанию 30 дней от сегодня)")
    public List<HotelOccupancy> allHotels(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.forAllHotels(from, to);
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "Детальная загруженность отеля с разбивкой по номерам")
    public HotelOccupancy hotel(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.forHotel(id, from, to);
    }
}
