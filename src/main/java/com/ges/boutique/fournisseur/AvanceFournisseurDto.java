package com.ges.boutique.fournisseur;

import com.ges.boutique.avance.StatutAvance;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AvanceFournisseurDto {
    private Long id;
    private Long fournisseurId;
    private String fournisseurNom;
    private Double montant;
    private Double montantUtilise;
    private Double montantDisponible;
    private LocalDateTime dateDepot;
    private String motif;
    private StatutAvance statut;
    private String sourceFinancement;
    private Long compteId;

    public static AvanceFournisseurDto fromEntity(AvanceFournisseur a) {
        AvanceFournisseurDto dto = new AvanceFournisseurDto();
        dto.setId(a.getId());
        dto.setFournisseurId(a.getFournisseur().getId());
        dto.setFournisseurNom(a.getFournisseur().getNom());
        dto.setMontant(a.getMontant());
        dto.setMontantUtilise(a.getMontantUtilise());
        dto.setMontantDisponible(a.getMontantDisponible());
        dto.setDateDepot(a.getDateDepot());
        dto.setMotif(a.getMotif());
        dto.setStatut(a.getStatut());
        dto.setSourceFinancement(a.getSourceFinancement());
        dto.setCompteId(a.getCompteId());
        return dto;
    }
}
