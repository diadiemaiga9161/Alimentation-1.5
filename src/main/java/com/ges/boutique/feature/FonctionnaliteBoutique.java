package com.ges.boutique.feature;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table dédiée, indépendante des colonnes feature_transferts_actif/feature_vitrine_actif
 * existantes sur Boutique — une ligne par fonctionnalité avancée (voir CleFonctionnalite).
 * Absence de ligne pour une clé = fonctionnalité active par défaut (voir FeatureToggleService),
 * pour qu'une boutique déjà en place ne perde aucune fonctionnalité tant que le super admin
 * n'a rien désactivé explicitement.
 */
@Entity
@Table(name = "fonctionnalite_boutique")
@Getter
@Setter
@NoArgsConstructor
public class FonctionnaliteBoutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // columnDefinition forcé en VARCHAR : sans ça, Hibernate crée un ENUM MySQL natif figé
    // sur les valeurs de CleFonctionnalite au moment de la création de la table, que
    // ddl-auto=update n'élargit ensuite JAMAIS — toute nouvelle clé plante en écriture
    // ("Data truncated for column 'cle'"). Voir migration V106 pour la colonne déjà en place.
    @Enumerated(EnumType.STRING)
    @Column(name = "cle", nullable = false, unique = true, columnDefinition = "VARCHAR(40)")
    private CleFonctionnalite cle;

    @Column(name = "actif", columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean actif = true;

    public FonctionnaliteBoutique(CleFonctionnalite cle, boolean actif) {
        this.cle = cle;
        this.actif = actif;
    }
}
