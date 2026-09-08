package com.ges.boutique.feature;

import com.ges.boutique.exception.FonctionnaliteDesactiveeException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Applique @RequireFeature : une méthode annotée directement l'emporte sur l'annotation
 * de classe (contrôle plus fin, ex: bloquer seulement la création/modification d'un
 * contrôleur dont la lecture reste annotée au niveau classe par autre chose... en
 * pratique ici : classe = tout bloquer, méthode = ne bloquer que celle-ci).
 */
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureToggleAspect {

    private final FeatureToggleService featureToggleService;

    @Around("@within(com.ges.boutique.feature.RequireFeature) || @annotation(com.ges.boutique.feature.RequireFeature)")
    public Object verifier(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        RequireFeature annotation = AnnotatedElementUtils.findMergedAnnotation(method, RequireFeature.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), RequireFeature.class);
        }
        if (annotation == null) {
            return joinPoint.proceed();
        }

        CleFonctionnalite cle = annotation.value();
        if (!featureToggleService.estActive(cle)) {
            throw new FonctionnaliteDesactiveeException(
                    "La fonctionnalité \"" + cle.getLibelle() + "\" est désactivée par le super admin de la boutique. "
                            + "Pour plus d'informations, contactez Maiga Consulting.");
        }
        return joinPoint.proceed();
    }
}
