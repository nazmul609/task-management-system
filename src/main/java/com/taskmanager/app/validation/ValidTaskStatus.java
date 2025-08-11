package com.taskmanager.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TaskStatusValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTaskStatus {

    String message() default "Status must be one of: TODO, IN_PROGRESS, DONE";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}