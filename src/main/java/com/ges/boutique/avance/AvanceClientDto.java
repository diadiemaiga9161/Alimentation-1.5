package com.ges.boutique.avance;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AvanceClientDto {
    private Long id;
    private String clientNom;
    private String clientTelephone;
    private Double montant;
    private Double montantUtilise;
    private Double montantDisponible;
    private LocalDateTime dateDepot;
    private String motif;
    private StatutAvance statut;

    public static AvanceClientDto fromEntity(AvanceClient a) {
        AvanceClientDto dto = new AvanceClientDto();
        dto.setId(a.getId());
        dto.setClientNom(a.getClientNom());
        dto.setClientTelephone(a.getClientTelephone());
        dto.setMontant(a.getMontant());
        dto.setMontantUtilise(a.getMontantUtilise());
        dto.setMontantDisponible(a.getMontantDisponible());
        dto.setDateDepot(a.getDateDepot());
        dto.setMotif(a.getMotif());
        dto.setStatut(a.getStatut());
        return dto;
    }
}
