package com.ges.boutique.objectifvendeur;

import com.ges.boutique.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "objectif_vendeur")
@Getter
@Setter
@NoArgsConstructor
public class ObjectifVendeur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Utilisateur vendeur;

    @Column(nullable = false)
    private Integer semaine;

    @Column(nullable = false)
    private Integer annee;

    @Column(nullable = false)
    private Integer objectifNombreVentes;

    @Column(nullable = false)
    private Double bonusMontant;

    @Column(nullable = false)
    private Boolean bonusValide = false;

    @Column
    private String observation;

    @Column
    private LocalDateTime dateValidation;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
