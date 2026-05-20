package com.ges.boutique.compte;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CompteDto {
    private Long id;
    private String nomBanque;
    private String numeroCompte;
    private String agence;
    private String titulaire;
    private Double soldeInitial;
    private Double totalVersements;
    private Double totalRetraits;
    private Double totalCheques;
    private Double totalFrais;
    private Double totalBonsCaisse;
    private Double soldeActuel;
    private boolean actif;
    private String description;
    private LocalDateTime dateCreation;

    public static CompteDto fromEntity(Compte c) {
        CompteDto dto = new CompteDto();
        dto.setId(c.getId());
        dto.setNomBanque(c.getNomBanque());
        dto.setNumeroCompte(c.getNumeroCompte());
        dto.setAgence(c.getAgence());
        dto.setTitulaire(c.getTitulaire());
        dto.setSoldeInitial(c.getSoldeInitial());
        dto.setTotalVersements(c.getTotalVersements());
        dto.setTotalRetraits(c.getTotalRetraits());
        dto.setTotalCheques(c.getTotalCheques());
        dto.setTotalFrais(c.getTotalFrais());
        dto.setTotalBonsCaisse(c.getTotalBonsCaisse());
        dto.setSoldeActuel(c.getSoldeActuel());
        dto.setActif(c.isActif());
        dto.setDescription(c.getDescription());
        dto.setDateCreation(c.getDateCreation());
        return dto;
    }
}
