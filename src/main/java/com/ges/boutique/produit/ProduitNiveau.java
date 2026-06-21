package com.ges.boutique.produit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "produit_niveaux")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitNiveau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    @JsonIgnore
    private Produit produit;

    @Column(nullable = false)
    private String nom; // "Carton", "Cartouche", "Pièce"

    @Column(nullable = false)
    private Integer ordre; // 1 = plus grand, dernier = unité de base

    @Column(nullable = false)
    private Integer facteur; // combien d'unités du niveau suivant (1 pour unité de base)

    @Column(name = "prix_achat", nullable = false)
    private Double prixAchat;

    @Column(name = "prix_vente", nullable = false)
    private Double prixVente;
}
