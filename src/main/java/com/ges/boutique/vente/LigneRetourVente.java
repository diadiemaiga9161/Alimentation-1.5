package com.ges.boutique.vente;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ges.boutique.produit.Produit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lignes_retour_vente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneRetourVente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retour_id", nullable = false)
    @JsonBackReference
    private RetourVente retour;

    @ManyToOne(fetch = FetchType.EAGER)  // ← CHANGEMENT IMPORTANT: EAGER au lieu de LAZY
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "ligne_vente_id")
    private Long ligneVenteId;

    @Column(nullable = false)
    private Integer quantiteRetournee;

    @Column(nullable = false)
    private Double prixUnitaire;

    @Column(nullable = false)
    private Double sousTotal;
}