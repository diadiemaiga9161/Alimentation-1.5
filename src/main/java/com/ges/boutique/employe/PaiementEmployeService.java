package com.ges.boutique.employe;

import java.util.List;
import java.util.Map;

public interface PaiementEmployeService {
    PaiementEmployeDto payerEmploye(PaiementEmployeRequest request);
    PaiementEmployeDto annulerPaiement(Long paiementId, String motifAnnulation, Long utilisateurId);
    PaiementEmployeDto getPaiementById(Long id);
    List<PaiementEmployeDto> getTousLesPaiements();
    List<PaiementEmployeDto> getPaiementsParEmploye(Long employeId);
    List<PaiementEmployeDto> getPaiementsActifs();
    Map<String, Object> getStatistiques();
}
