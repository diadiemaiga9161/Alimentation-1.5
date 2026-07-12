package com.ges.boutique.fournisseur;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ges.boutique.produit.Produit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lignes_retour_achat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneRetourAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retour_id", nullable = false)
    @JsonBackReference
    private RetourAchat retour;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "ligne_achat_id")
    private Long ligneAchatId;

    @Column(name = "quantite_retournee", nullable = false)
    private Integer quantiteRetournee;

    @Column(name = "prix_unitaire", nullable = false)
    private Double prixUnitaire;

    @Column(name = "sous_total", nullable = false)
    private Double sousTotal;

    @PrePersist
    @PreUpdate
    private void calculer() {
        if (quantiteRetournee != null && prixUnitaire != null) {
            sousTotal = quantiteRetournee * prixUnitaire;
        }
    }
}
