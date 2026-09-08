package com.ges.boutique.feature;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeatureToggleService {

    private final FonctionnaliteBoutiqueRepository repository;

    /**
     * Lot d'origine de fonctionnalités avancées (premières ajoutées à ce système) :
     * absence de ligne pour l'une d'elles = ACTIVE par défaut, pour qu'une boutique déjà
     * en place (table vide au premier démarrage après mise à jour) ne perde aucune
     * fonctionnalité qu'elle utilisait déjà, tant que le super admin n'a rien désactivé
     * lui-même.
     *
     * TOUTE nouvelle clé ajoutée à CleFonctionnalite APRÈS ce lot ne doit PAS être
     * ajoutée ici : elle doit rester INACTIVE tant que le super admin ne l'a pas
     * explicitement activée pour une boutique (règle donnée par l'utilisateur le
     * 2026-09-02 : chaque nouvelle fonction reste désactivée par défaut, jamais active
     * par défaut — contrairement au lot d'origine). Ne JAMAIS ajouter une clé à cet
     * ensemble après coup : ça reviendrait à activer sans prévenir une fonctionnalité
     * sur toutes les boutiques déjà en place.
     */
    private static final Set<CleFonctionnalite> ACTIVES_PAR_DEFAUT = EnumSet.of(
            CleFonctionnalite.DEPOT_GARDE,
            CleFonctionnalite.DETTES_ANCIENNES,
            CleFonctionnalite.COMPTES_BANCAIRES,
            CleFonctionnalite.PROMOTIONS,
            CleFonctionnalite.MOBILE_MONEY,
            CleFonctionnalite.BONUS_FOURNISSEURS,
            CleFonctionnalite.OBJECTIFS_FOURNISSEUR,
            CleFonctionnalite.OBJECTIFS_VENDEUR,
            CleFonctionnalite.RAPPORTS,
            CleFonctionnalite.IA,
            CleFonctionnalite.RESULTAT_NET
    );

    /**
     * Mise en cache (30s, voir CacheConfig) car vérifiée à chaque requête concernée
     * via @RequireFeature — éviter une requête base à chaque appel.
     */
    @Cacheable("fonctionnalites")
    public boolean estActive(CleFonctionnalite cle) {
        boolean defautSiAbsente = ACTIVES_PAR_DEFAUT.contains(cle);
        return repository.findByCle(cle).map(FonctionnaliteBoutique::isActif).orElse(defautSiAbsente);
    }

    /** État de toutes les fonctionnalités connues, pour la page super admin. */
    public List<Map<String, Object>> obtenirToutes() {
        return Arrays.stream(CleFonctionnalite.values())
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
    @CacheEvict(value = "fonctionnalites", allEntries = true)
    public void definirEtat(CleFonctionnalite cle, boolean actif) {
        FonctionnaliteBoutique f = repository.findByCle(cle)
                .orElseGet(() -> new FonctionnaliteBoutique(cle, true));
        f.setActif(actif);
        repository.save(f);
    }
}
