package com.taskmanager.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UsernameValidator.class)  // Links to our validator class
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {

    String message() default "Username must start with a letter, contain only letters/numbers/underscores, and not end with underscore";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}