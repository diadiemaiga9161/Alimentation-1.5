package com.ges.boutique.objectif;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ObjectifFournisseurDto {
    private Long id;
    private Long fournisseurId;
    private String fournisseurNom;
    private Long produitId;
    private String produitNom;
    private Integer mois;
    private Integer annee;
    private Double objectifQuantite;
    private Double bonusParUnite;
    private Double quantiteAtteinte;
    private Double bonusCalcule;
    private Double quantiteBonusRecue;
    private StatutObjectif statut;
    private String observation;
    private Boolean stockAjoute;
    private LocalDateTime dateValidation;
    private LocalDateTime dateCreation;

    public static ObjectifFournisseurDto fromEntity(ObjectifFournisseur e) {
        ObjectifFournisseurDto dto = new ObjectifFournisseurDto();
        dto.setId(e.getId());
        dto.setFournisseurId(e.getFournisseur().getId());
        dto.setFournisseurNom(e.getFournisseur().getNom());
        if (e.getProduit() != null) {
            dto.setProduitId(e.getProduit().getId());
            dto.setProduitNom(e.getProduit().getNom());
        }
        dto.setMois(e.getMois());
        dto.setAnnee(e.getAnnee());
        dto.setObjectifQuantite(e.getObjectifQuantite());
        dto.setBonusParUnite(e.getBonusParUnite());
        dto.setQuantiteAtteinte(e.getQuantiteAtteinte());
        dto.setBonusCalcule(e.getBonusCalcule());
        dto.setQuantiteBonusRecue(e.getQuantiteBonusRecue());
        dto.setStatut(e.getStatut());
        dto.setObservation(e.getObservation());
        dto.setStockAjoute(e.getStockAjoute());
        dto.setDateValidation(e.getDateValidation());
        dto.setDateCreation(e.getDateCreation());
        return dto;
    }
}
