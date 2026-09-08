package com.ges.boutique.permission;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionVendeurService {

    private final PermissionVendeurRepository repository;

    /**
     * Absence de ligne pour une clé = permission INACTIVE par défaut, pour que le
     * vendeur n'obtienne aucun accès supplémentaire tant que l'admin de la boutique n'a
     * rien accordé explicitement. Mise en cache (30s, voir CacheConfig) car vérifiée à
     * chaque requête concernée via @PreAuthorize — éviter une requête base à chaque appel.
     */
    @Cacheable("permissionsVendeur")
    public boolean estActive(CleVendeur cle) {
        return repository.findByCle(cle).map(PermissionVendeur::isActif).orElse(false);
    }

    /** État de toutes les permissions connues, pour la page réglages de la boutique. */
    public List<Map<String, Object>> obtenirToutes() {
        return Arrays.stream(CleVendeur.values())
                .map(cle -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("cle", cle.name());
                    m.put("libelle", cle.getLibelle());
                    m.put("actif", estActive(cle));
                    return m;
                })
                .toList();
    }

    @Transactional
    @CacheEvict(value = "permissionsVendeur", allEntries = true)
    public void definirEtat(CleVendeur cle, boolean actif) {
        PermissionVendeur p = repository.findByCle(cle)
                .orElseGet(() -> new PermissionVendeur(cle, false));
        p.setActif(actif);
        repository.save(p);
    }
}
