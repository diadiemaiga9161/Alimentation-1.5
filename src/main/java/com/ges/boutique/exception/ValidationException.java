package com.ges.boutique.exception;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
    private final String field;

    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    public ValidationException(String message, String field) {
        super(message);
        this.field = field;
    }
}