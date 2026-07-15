package com.ges.boutique.ia;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "parametre_modele_ia")
@Data
@NoArgsConstructor
public class ParametreModeleIA {

    @Id
    private String cle; // ex: "seuil_alerte_stock", "alpha_ema", "poids_recence_rfm"

    @Column(nullable = false)
    private double valeur;

    /**
     * Taux de précision des dernières prédictions (0-1).
     * Nommé "precision_modele" en base pour éviter le mot réservé SQL "precision".
     */
    @Column(name = "precision_modele")
    private double precisionModele;

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false)
    private LocalDateTime dateMiseAJour = LocalDateTime.now();

    public ParametreModeleIA(String cle, double valeur) {
        this.cle = cle;
        this.valeur = valeur;
        this.precisionModele = 0.0;
        this.version = 1;
        this.dateMiseAJour = LocalDateTime.now();
    }
}
