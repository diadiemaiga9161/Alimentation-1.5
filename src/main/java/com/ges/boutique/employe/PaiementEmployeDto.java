package com.ges.boutique.employe;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaiementEmployeDto {
    private Long id;
    private Long employeId;
    private String employeNomComplet;
    private String employePoste;
    private Double salaireMensuel;
    private Double montant;
    private Integer nombreMois;
    private String periodeDebut;
    private String periodeFin;
    private LocalDateTime datePaiement;
    private StatutPaiementEmploye statut;
    private Long utilisateurId;
    private Long operationCaisseId;
    private String motifAnnulation;
    private LocalDateTime dateAnnulation;
    private String observation;

    public static PaiementEmployeDto fromEntity(PaiementEmploye p) {
        PaiementEmployeDto dto = new PaiementEmployeDto();
        dto.setId(p.getId());
        dto.setEmployeId(p.getEmploye().getId());
        String nomComplet = (p.getEmploye().getPrenom() != null ? p.getEmploye().getPrenom() + " " : "")
                + p.getEmploye().getNom();
        dto.setEmployeNomComplet(nomComplet);
        dto.setEmployePoste(p.getEmploye().getPoste());
        dto.setSalaireMensuel(p.getEmploye().getSalaireMensuel());
        dto.setMontant(p.getMontant());
        dto.setNombreMois(p.getNombreMois());
        dto.setPeriodeDebut(p.getPeriodeDebut());
        dto.setPeriodeFin(p.getPeriodeFin());
        dto.setDatePaiement(p.getDatePaiement());
        dto.setStatut(p.getStatut());
        dto.setUtilisateurId(p.getUtilisateurId());
        dto.setOperationCaisseId(p.getOperationCaisseId());
        dto.setMotifAnnulation(p.getMotifAnnulation());
        dto.setDateAnnulation(p.getDateAnnulation());
        dto.setObservation(p.getObservation());
        return dto;
    }
}
