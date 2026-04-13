package com.ges.boutique.vente;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ges.boutique.produit.Produit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "lignes_vente")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LigneVente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vente_id", nullable = false)
    @JsonBackReference
    private Vente vente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "prix_unitaire", nullable = false)
    private Double prixUnitaire;

    @Column(name = "remise_pourcentage")
    private Double remisePourcentage = 0.0;

    @Column(name = "remise_montant")
    private Double remiseMontant = 0.0;

    @Column(name = "prix_apres_remise")
    private Double prixApresRemise;

    @Column(name = "sous_total", nullable = false)
    private Double sousTotal;

    @PrePersist
    @PreUpdate
    protected void calculerSousTotal() {
        // Initialiser les valeurs si elles sont null
        if (prixUnitaire == null) prixUnitaire = 0.0;
        if (quantite == null) quantite = 0;
        if (remisePourcentage == null) remisePourcentage = 0.0;
        if (remiseMontant == null) remiseMontant = 0.0;

        // Calculer le prix après remise
        Double prixBase = prixUnitaire;

        if (remisePourcentage > 0) {
            // Remise en pourcentage
            Double reduction = prixBase * (remisePourcentage / 100);
            prixApresRemise = prixBase - reduction;
        } else if (remiseMontant > 0) {
            // Remise en montant fixe
            prixApresRemise = Math.max(0, prixBase - remiseMontant);
        } else {
            // Pas de remise
            prixApresRemise = prixBase;
        }

        // Assurer que le prix après remise n'est pas négatif
        if (prixApresRemise < 0) {
            prixApresRemise = 0.0;
        }

        // Calculer le sous-total
        sousTotal = prixApresRemise * quantite;

        // Arrondir à 2 décimales
        sousTotal = BigDecimal.valueOf(sousTotal)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        prixApresRemise = BigDecimal.valueOf(prixApresRemise)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // Méthode pour appliquer une remise
    public void appliquerRemisePourcentage(Double pourcentage) {
        if (pourcentage != null && pourcentage >= 0 && pourcentage <= 100) {
            this.remisePourcentage = pourcentage;
            this.remiseMontant = 0.0;
            calculerSousTotal();
        }
    }

    public void appliquerRemiseMontant(Double montant) {
        if (montant != null && montant >= 0) {
            this.remiseMontant = montant;
            this.remisePourcentage = 0.0;
            calculerSousTotal();
        }
    }

    // Méthode pour obtenir le montant de la remise
    public Double getMontantRemise() {
        if (prixUnitaire == null || quantite == null) return 0.0;

        Double totalSansRemise = prixUnitaire * quantite;
        Double remise = totalSansRemise - sousTotal;
        return BigDecimal.valueOf(Math.max(0, remise))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // Méthode utilitaire pour les DTO
    public String getProduitNom() {
        return produit != null ? produit.getNom() : null;
    }

    public Long getProduitId() {
        return produit != null ? produit.getId() : null;
    }
}