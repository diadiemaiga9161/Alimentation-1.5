package com.ges.boutique.exception;

/**
 * Dictionnaire centralisé des codes d'erreur de l'API.
 * Utilisé avec ApiError pour renvoyer des réponses structurées.
 */
public enum ErrorCode {
    WRONG_PASSWORD,
    USER_NOT_FOUND,
    INSUFFICIENT_STOCK,
    OPTIMISTIC_LOCK_CONFLICT,
    BOUTIQUE_NOT_FOUND,
    PRODUIT_NOT_FOUND,
    CREDIT_ALREADY_SETTLED,
    TRANSFER_NOT_FOUND,
    UNAUTHORIZED,
    VALIDATION_ERROR,
    INTERNAL_ERROR,
    RESOURCE_NOT_FOUND,
    DATA_INTEGRITY_VIOLATION,
    ACCOUNT_DISABLED,
    ACCOUNT_LOCKED,
    TOKEN_EXPIRED,
    INVALID_TOKEN,
    MISSING_PARAMETER,
    TYPE_MISMATCH
}
