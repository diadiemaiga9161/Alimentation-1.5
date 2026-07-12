package com.ges.boutique.depot;

import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ClientDepotGroupeDto {

    private String numero;
    private String nom;
    private String prenom;
    private String nomComplet;
    private int nombreDepotsActifs;
    private Double totalMontantInitial;
    private Double totalMontantRestant;
    private Double totalMontantRetire;
    private List<DepotGardeDto> depots;

    public static ClientDepotGroupeDto fromDepots(List<DepotGarde> depots) {
        ClientDepotGroupeDto dto = new ClientDepotGroupeDto();
        if (depots == null || depots.isEmpty()) return dto;

        DepotGarde premier = depots.get(0);
        dto.setNumero(premier.getNumero());
        dto.setNom(premier.getNom());
        dto.setPrenom(premier.getPrenom());
        dto.setNomComplet((premier.getPrenom() != null ? premier.getPrenom() + " " : "") + premier.getNom());
        dto.setNombreDepotsActifs(depots.size());
        dto.setTotalMontantInitial(depots.stream().mapToDouble(DepotGarde::getMontantInitial).sum());
        dto.setTotalMontantRestant(depots.stream().mapToDouble(DepotGarde::getMontantRestant).sum());
        dto.setTotalMontantRetire(dto.getTotalMontantInitial() - dto.getTotalMontantRestant());
        dto.setDepots(depots.stream().map(DepotGardeDto::fromEntity).collect(Collectors.toList()));
        return dto;
    }
}
