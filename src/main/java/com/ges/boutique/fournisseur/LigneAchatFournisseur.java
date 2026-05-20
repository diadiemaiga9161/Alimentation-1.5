package com.ges.boutique.fournisseur;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ges.boutique.produit.Produit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lignes_achat_fournisseur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneAchatFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achat_id", nullable = false)
    @JsonBackReference
    private AchatFournisseur achat;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "prix_achat_unitaire", nullable = false)
    private Double prixAchatUnitaire;

    @Column(name = "sous_total", nullable = false)
    private Double sousTotal;

    @PrePersist
    @PreUpdate
    private void autoCalculSousTotal() {
        if (quantite != null && prixAchatUnitaire != null) {
            this.sousTotal = quantite * prixAchatUnitaire;
        } else {
            this.sousTotal = 0.0;
        }
    }
}