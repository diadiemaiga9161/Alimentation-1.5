package com.ges.boutique.ia;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "profil_ia")
@Data
public class ProfilIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String typeBoutique; // ALIMENTATION, TEXTILE, ELECTRONIQUE, MIXTE, AUTRE

    @Column(columnDefinition = "TEXT")
    private String joursApprovisionnement; // JSON: ["LUNDI","MERCREDI","VENDREDI"]

    @Column(nullable = false)
    private int objectifStockJours = 30;

    @Column(nullable = false)
    private double margeObjectif = 20.0;

    @Column(nullable = false)
    private int delaiReglementCredit = 30;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column
    private LocalDateTime dateMiseAJour;
}
