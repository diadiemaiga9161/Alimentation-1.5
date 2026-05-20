package com.ges.boutique.compte;

import java.util.List;

public interface CompteService {
    Compte creerCompte(CompteRequest request);
    Compte modifierCompte(Long id, CompteRequest request);
    List<Compte> getTousLesComptes();
    Compte getCompteById(Long id);
    OperationCompte enregistrerOperation(OperationCompteRequest request);
    List<OperationCompte> getHistoriqueOperations(Long compteId);
    void debiterCompte(Long compteId, Double montant, String motif, TypeOperationCompte type, Long utilisateurId);
    void crediterCompte(Long compteId, Double montant, String motif, TypeOperationCompte type, Long utilisateurId);
}
