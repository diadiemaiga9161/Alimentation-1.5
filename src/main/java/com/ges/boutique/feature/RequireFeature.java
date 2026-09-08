package com.ges.boutique.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pose sur un contrôleur (toutes ses méthodes) ou une méthode précise (pour ne bloquer
 * que certaines actions, ex: écriture seule sur Dépôt de garde / Dettes anciennes tout
 * en laissant la lecture) : refuse la requête (403) si le super admin a désactivé cette
 * fonctionnalité pour la boutique — voir FeatureToggleAspect.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireFeature {
    CleFonctionnalite value();
}
