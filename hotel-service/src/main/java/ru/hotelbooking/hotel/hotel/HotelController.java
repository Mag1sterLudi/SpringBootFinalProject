package ru.hotelbooking.hotel.hotel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hotelbooking.common.web.PageResponse;
import ru.hotelbooking.hotel.hotel.dto.HotelFilter;
import ru.hotelbooking.hotel.hotel.dto.HotelRequest;
import ru.hotelbooking.hotel.hotel.dto.HotelResponse;

@Tag(name = "Hotels")
@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Добавить отель")
    public HotelResponse create(@Valid @RequestBody HotelRequest request) {
        return hotelService.create(request);
    }

    @GetMapping
    @Operation(summary = "Список отелей с фильтрами по городу, названию и звёздности")
    public PageResponse<HotelResponse> search(@Valid @ParameterObject HotelFilter filter,
                                              @ParameterObject @PageableDefault(size = 20, sort = "id")
                                              Pageable pageable) {
        return hotelService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Отель по идентификатору")
    public HotelResponse getById(@PathVariable Long id) {
        return hotelService.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Обновить отель")
    public HotelResponse update(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        return hotelService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить отель без истории бронирований")
    public void delete(@PathVariable Long id) {
        hotelService.delete(id);
    }
}
