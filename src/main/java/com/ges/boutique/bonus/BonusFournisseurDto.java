package com.ges.boutique.bonus;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class BonusFournisseurDto {
    private Long id;
    private Long fournisseurId;
    private String fournisseurNom;
    private TypeBonus type;
    private String typeLibelle;
    private Double montant;
    private Long produitId;
    private String produitNom;
    private Double quantiteProduit;
    private LocalDate date;
    private String description;
    private LocalDateTime dateCreation;

    public static BonusFournisseurDto from(BonusFournisseur b) {
        BonusFournisseurDto dto = new BonusFournisseurDto();
        dto.setId(b.getId());
        dto.setFournisseurId(b.getFournisseur().getId());
        dto.setFournisseurNom(b.getFournisseur().getNom());
        dto.setType(b.getType());
        dto.setTypeLibelle(libelleType(b.getType()));
        dto.setMontant(b.getMontant());
        if (b.getProduit() != null) {
            dto.setProduitId(b.getProduit().getId());
            dto.setProduitNom(b.getProduit().getNom());
        }
        dto.setQuantiteProduit(b.getQuantiteProduit());
        dto.setDate(b.getDate());
        dto.setDescription(b.getDescription());
        dto.setDateCreation(b.getDateCreation());
        return dto;
    }

    private static String libelleType(TypeBonus type) {
        return switch (type) {
            case RISTOURNE -> "Ristourne";
            case BONUS_VOLUME -> "Bonus Volume";
            case PRIME_OBJECTIF -> "Prime Objectif";
            case BONUS_ACHAT -> "Bonus Achat";
        };
    }
}
