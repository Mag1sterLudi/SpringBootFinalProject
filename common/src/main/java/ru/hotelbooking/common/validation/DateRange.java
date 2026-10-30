package ru.hotelbooking.common.validation;

import java.time.LocalDate;

/**
 * Период проживания с startDate по endDate, день выезда не входит.
 */
public interface DateRange {

    LocalDate startDate();

    LocalDate endDate();
}
