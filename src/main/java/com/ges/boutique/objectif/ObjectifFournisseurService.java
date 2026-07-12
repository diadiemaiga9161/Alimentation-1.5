package com.ges.boutique.objectif;

import java.util.List;
import java.util.Map;

public interface ObjectifFournisseurService {

    ObjectifFournisseurDto creer(ObjectifFournisseurRequest request);

    ObjectifFournisseurDto modifier(Long id, ObjectifFournisseurRequest request);

    ObjectifFournisseurDto getById(Long id);

    List<ObjectifFournisseurDto> getTous();

    List<ObjectifFournisseurDto> getParMoisAnnee(int mois, int annee);

    List<ObjectifFournisseurDto> getParFournisseur(Long fournisseurId);

    List<ObjectifFournisseurDto> getParAnnee(int annee);

    ObjectifFournisseurDto valider(Long id);

    void supprimer(Long id);

    StatsObjectifDto getStatistiques(int mois, int annee);

    Map<String, Object> getRapportMensuel(int mois, int annee);

    Map<String, Object> getRapportAnnuel(int annee);
}
