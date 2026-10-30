package ru.hotelbooking.common.validation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import ru.hotelbooking.common.error.BadRequestException;

public final class DateRanges {

    public static final int MAX_NIGHTS = 30;

    private DateRanges() {
    }

    public static Optional<String> findProblem(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            return Optional.of("endDate must be after startDate");
        }
        if (nights(startDate, endDate) > MAX_NIGHTS) {
            return Optional.of("stay must not exceed " + MAX_NIGHTS + " nights");
        }
        return Optional.empty();
    }

    /**
     * Для query-параметров - даты передаются либо обе, либо ни одной.
     */
    public static void requireValidOptional(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return;
        }
        if (startDate == null || endDate == null) {
            throw new BadRequestException("INVALID_DATE_RANGE", "startDate and endDate must be passed together");
        }
        findProblem(startDate, endDate).ifPresent(message -> {
            throw new BadRequestException("INVALID_DATE_RANGE", message);
        });
    }

    public static long nights(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Сколько ночей периода с start по end попадает в период с from по to.
     */
    public static long overlapNights(LocalDate start, LocalDate end, LocalDate from, LocalDate to) {
        LocalDate overlapStart = start.isAfter(from) ? start : from;
        LocalDate overlapEnd = end.isBefore(to) ? end : to;
        return Math.max(0, nights(overlapStart, overlapEnd));
    }
}
