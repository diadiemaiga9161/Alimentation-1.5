package com.ges.boutique.objectif;

import com.ges.boutique.fournisseur.Fournisseur;
import com.ges.boutique.produit.Produit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "objectif_fournisseur")
@Getter
@Setter
@NoArgsConstructor
public class ObjectifFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @Column(nullable = false)
    private Integer mois;

    @Column(nullable = false)
    private Integer annee;

    @Column(nullable = false)
    private Double objectifQuantite;

    @Column(nullable = false)
    private Double bonusParUnite;

    @Column(nullable = false)
    private Double quantiteAtteinte = 0.0;

    @Column(nullable = false)
    private Double bonusCalcule = 0.0;

    @Column(nullable = false)
    private Double quantiteBonusRecue = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutObjectif statut = StatutObjectif.NON_ATTEINT;

    @Column
    private String observation;

    @Column(nullable = false)
    private Boolean stockAjoute = false;

    @Column
    private LocalDateTime dateValidation;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
