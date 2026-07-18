package com.ges.boutique.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Réponse d'erreur structurée pour l'API.
 * Fournit un code machine (ErrorCode) et un message lisible par l'humain.
 */
@Data
@AllArgsConstructor
public class ApiError {

    private String code;
    private String message;
    private Object details;
    private LocalDateTime timestamp;

    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code.name(), message, null, LocalDateTime.now());
    }

    public static ApiError of(ErrorCode code, String message, Object details) {
        return new ApiError(code.name(), message, details, LocalDateTime.now());
    }
}
