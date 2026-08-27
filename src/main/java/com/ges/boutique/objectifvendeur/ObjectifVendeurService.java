package com.ges.boutique.objectifvendeur;

import java.util.List;

public interface ObjectifVendeurService {

    ObjectifVendeurDto creer(ObjectifVendeurRequest request);

    ObjectifVendeurDto modifier(Long id, ObjectifVendeurRequest request);

    ObjectifVendeurDto getById(Long id);

    List<ObjectifVendeurDto> getTous();

    List<ObjectifVendeurDto> getParSemaineAnnee(int semaine, int annee);

    List<ObjectifVendeurDto> getParVendeur(Long vendeurId);

    List<ObjectifVendeurDto> getParAnnee(int annee);

    ObjectifVendeurDto valider(Long id);

    void supprimer(Long id);
}
