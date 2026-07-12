package com.ges.boutique.depense;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DepenseService {
    Depense creerDepense(DepenseRequest request, Long utilisateurId);
    Depense modifierDepense(Long id, DepenseRequest request, Long utilisateurId);
    Depense obtenirParId(Long id);
    List<Depense> obtenirToutes();
    List<Depense> obtenirParPeriode(LocalDate debut, LocalDate fin);
    void supprimerDepense(Long id, Long utilisateurId);
    Double getTotalDepenses();
    Double getTotalDepensesParPeriode(LocalDate debut, LocalDate fin);
    Map<String, Double> getTotauxParType();
    Map<String, Double> getTotauxParTypePeriode(LocalDate debut, LocalDate fin);
}
