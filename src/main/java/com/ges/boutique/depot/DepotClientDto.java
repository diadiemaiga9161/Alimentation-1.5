package com.ges.boutique.depot;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DepotClientDto {
    private Long id;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String numero;
    private String adresse;
    private String observation;
    private LocalDateTime dateCreation;

    public static DepotClientDto fromEntity(DepotClient e) {
        DepotClientDto dto = new DepotClientDto();
        dto.setId(e.getId());
        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        dto.setNomComplet((e.getPrenom() != null ? e.getPrenom() + " " : "") + e.getNom());
        dto.setNumero(e.getNumero());
        dto.setAdresse(e.getAdresse());
        dto.setObservation(e.getObservation());
        dto.setDateCreation(e.getDateCreation());
        return dto;
    }
}
