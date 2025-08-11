package com.taskmanager.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    @Override
    public void initialize(ValidUsername constraintAnnotation) {
        // Initialization logic if needed
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull, so we allow null here
        if (username == null) {
            return true;
        }

        // Check length (3-20 characters)
        if (username.length() < 3 || username.length() > 20) {
            return false;
        }

        // Must start with a letter
        if (!Character.isLetter(username.charAt(0))) {
            return false;
        }

        // Cannot end with underscore
        if (username.endsWith("_")) {
            return false;
        }

        // Can only contain letters, numbers, and underscores
        return username.matches("^[a-zA-Z][a-zA-Z0-9_]*[a-zA-Z0-9]$|^[a-zA-Z]$");
    }
}