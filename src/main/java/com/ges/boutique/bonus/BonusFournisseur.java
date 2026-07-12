package com.ges.boutique.bonus;

import com.ges.boutique.fournisseur.Fournisseur;
import com.ges.boutique.produit.Produit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bonus_fournisseur")
@Getter
@Setter
@NoArgsConstructor
public class BonusFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeBonus type;

    @Column(nullable = false)
    private Double montant = 0.0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @Column
    private Double quantiteProduit;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private String description;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
