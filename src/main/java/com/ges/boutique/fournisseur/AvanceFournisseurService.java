package com.ges.boutique.fournisseur;

import java.util.List;

public interface AvanceFournisseurService {
    AvanceFournisseur enregistrerAvance(AvanceFournisseurRequest request);
    Double getSoldeDisponible(Long fournisseurId);
    List<AvanceFournisseur> getHistoriqueParFournisseur(Long fournisseurId);
    List<AvanceFournisseur> getToutesLesAvances();
    void utiliserAvance(Long fournisseurId, Double montantAUtiliser);
    void annulerUtilisationAvance(Long fournisseurId, Double montantAAnnuler);
}