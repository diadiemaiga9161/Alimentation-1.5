package com.ges.boutique.depot;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DepotGardeDto {
    private Long id;
    private Long depotClientId;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String numero;
    private Double montantInitial;
    private Double montantRestant;
    private Double montantRetire;
    private StatutDepot statut;
    private LocalDateTime dateDepot;
    private String observation;
    private List<RetraitDepotDto> retraits;

    public static DepotGardeDto fromEntity(DepotGarde d) {
        DepotGardeDto dto = new DepotGardeDto();
        dto.setId(d.getId());
        dto.setNom(d.getNom());
        dto.setPrenom(d.getPrenom());
        dto.setNomComplet((d.getPrenom() != null ? d.getPrenom() + " " : "") + d.getNom());
        dto.setNumero(d.getNumero());
        dto.setMontantInitial(d.getMontantInitial());
        dto.setMontantRestant(d.getMontantRestant());
        dto.setMontantRetire(d.getMontantInitial() - d.getMontantRestant());
        dto.setStatut(d.getStatut());
        dto.setDateDepot(d.getDateDepot());
        dto.setObservation(d.getObservation());
        if (d.getDepotClient() != null) dto.setDepotClientId(d.getDepotClient().getId());
        dto.setRetraits(d.getRetraits().stream()
                .map(RetraitDepotDto::fromEntity)
                .collect(Collectors.toList()));
        return dto;
    }
}
