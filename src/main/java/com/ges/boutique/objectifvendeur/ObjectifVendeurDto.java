package com.ges.boutique.objectifvendeur;

import com.ges.boutique.objectif.StatutObjectif;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ObjectifVendeurDto {
    private Long id;
    private Long vendeurId;
    private String vendeurNom;
    private Integer semaine;
    private Integer annee;
    private Integer objectifNombreVentes;
    private Double bonusMontant;
    private Long nombreVentesAtteint;
    private StatutObjectif statut;
    private Boolean bonusValide;
    private String observation;
    private LocalDateTime dateValidation;
    private LocalDateTime dateCreation;
}
