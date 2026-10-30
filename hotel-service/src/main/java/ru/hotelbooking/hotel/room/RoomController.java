package ru.hotelbooking.hotel.room;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hotelbooking.common.web.PageResponse;
import ru.hotelbooking.hotel.room.dto.RoomRequest;
import ru.hotelbooking.hotel.room.dto.RoomResponse;
import ru.hotelbooking.hotel.room.dto.RoomSearchCriteria;

@Tag(name = "Rooms")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Добавить номер в отель")
    public RoomResponse create(@Valid @RequestBody RoomRequest request) {
        return roomService.create(request);
    }

    @GetMapping
    @Operation(summary = "Свободные номера (фильтры, пагинация, произвольная сортировка)")
    public PageResponse<RoomResponse> findFree(@Valid @ParameterObject RoomSearchCriteria criteria,
                                               @ParameterObject @PageableDefault(size = 20, sort = "id")
                                               Pageable pageable) {
        return roomService.findFree(criteria, pageable);
    }

    @GetMapping("/recommend")
    @Operation(summary = "Рекомендованные номера: свободные, по возрастанию times_booked, затем по id")
    public List<RoomResponse> recommend(@Valid @ParameterObject RoomSearchCriteria criteria,
                                        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return roomService.recommend(criteria, limit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Номер по идентификатору")
    public RoomResponse getById(@PathVariable Long id) {
        return roomService.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Обновить номер")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить номер без истории бронирований")
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
