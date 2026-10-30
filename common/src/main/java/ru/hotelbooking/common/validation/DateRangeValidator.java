package ru.hotelbooking.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, DateRange> {

    @Override
    public boolean isValid(DateRange value, ConstraintValidatorContext context) {
        if (value == null || value.startDate() == null || value.endDate() == null) {
            // null ловит @NotNull
            return true;
        }
        Optional<String> problem = DateRanges.findProblem(value.startDate(), value.endDate());
        if (problem.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(problem.get())
                .addPropertyNode("endDate")
                .addConstraintViolation();
        return false;
    }
}
