package com.ges.boutique.employe;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeRequest {
    private String nom;
    private String prenom;
    private String poste;
    private Double salaireMensuel;
    private String telephone;
    private String observation;
    private StatutEmploye statut;
    private LocalDate dateEmbauche;
}
