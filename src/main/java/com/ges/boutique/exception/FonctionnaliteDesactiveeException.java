package com.ges.boutique.exception;

/**
 * Levée par FeatureToggleAspect (@RequireFeature) quand le super admin a désactivé
 * la fonctionnalité concernée pour cette boutique — voir com.ges.boutique.feature.
 */
public class FonctionnaliteDesactiveeException extends RuntimeException {
    public FonctionnaliteDesactiveeException(String message) {
        super(message);
    }
}
