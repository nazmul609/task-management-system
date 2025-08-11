package com.taskmanager.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class TaskStatusValidator implements ConstraintValidator<ValidTaskStatus, String> {

    private static final List<String> VALID_STATUSES = Arrays.asList("TODO", "IN_PROGRESS", "DONE");

    @Override
    public void initialize(ValidTaskStatus constraintAnnotation) {
        // Initialization logic if needed
    }

    @Override
    public boolean isValid(String status, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull/@NotBlank
        if (status == null) {
            return true;
        }

        return VALID_STATUSES.contains(status.toUpperCase());
    }
}