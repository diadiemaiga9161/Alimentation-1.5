package com.ges.boutique.depot;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RetraitDepotDto {
    private Long id;
    private Double montant;
    private LocalDateTime dateRetrait;
    private String observation;

    public static RetraitDepotDto fromEntity(RetraitDepot r) {
        RetraitDepotDto dto = new RetraitDepotDto();
        dto.setId(r.getId());
        dto.setMontant(r.getMontant());
        dto.setDateRetrait(r.getDateRetrait());
        dto.setObservation(r.getObservation());
        return dto;
    }
}
