package com.ges.boutique.vitrine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO public et minimal pour les infos boutique affichées sur la vitrine.
 * ATTENTION SÉCURITÉ : exposé SANS authentification — ne jamais y ajouter
 * numeroRc, numeroIfu ou toute autre donnée administrative interne.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VitrineInfoDto {

    private String nom;
    private String adresse;
    private String telephone;
    private String horairesOuverture;
    private String logoPath;

    /** Si false, la vitrine (Boutique > Paramètres) a été désactivée par le super admin. */
    private boolean vitrineActive;
}
