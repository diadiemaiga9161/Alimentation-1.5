package com.ges.boutique.employe;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmployeDto {
    private Long id;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String poste;
    private Double salaireMensuel;
    private String telephone;
    private String observation;
    private StatutEmploye statut;
    private LocalDate dateEmbauche;
    private LocalDateTime dateCreation;

    public static EmployeDto fromEntity(Employe e) {
        EmployeDto dto = new EmployeDto();
        dto.setId(e.getId());
        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        dto.setNomComplet((e.getPrenom() != null ? e.getPrenom() + " " : "") + e.getNom());
        dto.setPoste(e.getPoste());
        dto.setSalaireMensuel(e.getSalaireMensuel());
        dto.setTelephone(e.getTelephone());
        dto.setObservation(e.getObservation());
        dto.setStatut(e.getStatut());
        dto.setDateEmbauche(e.getDateEmbauche());
        dto.setDateCreation(e.getDateCreation());
        return dto;
    }
}
