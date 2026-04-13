package com.ges.boutique.caisse;

import com.ges.boutique.vente.Vente;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CaisseService {
    Caisse ouvrirCaisse();
    Caisse fermerCaisse(Long utilisateurId);
    Caisse getCaisseOuverte();
    Double getSoldeActuel();
    Double getSoldeSysteme();
    Caisse verifierCaisse(Double soldeReelSaisi, Long utilisateurId, String observations);
    Map<String, Object> getEcartCaisse();
    OperationCaisse entreeCaisse(Double montant, String motif, Long utilisateurId,
                                 String modePaiement, String reference);
    OperationCaisse sortieCaisse(Double montant, String motif, Long utilisateurId);
    OperationCaisse enregistrerVente(Vente vente, Long utilisateurId,
                                     String modePaiement, String reference);
    OperationCaisse enregistrerVenteCredit(Vente vente, Long utilisateurId,
                                           String clientNom, String clientTelephone,
                                           LocalDate dateEcheance);
    OperationCaisse reglementCredit(Long venteCreditId, Double montantRegle,
                                    Long utilisateurId, String modePaiement, String reference);
    List<OperationCaisse> getCreditsNonRegles();
    List<OperationCaisse> getCreditsEnRetard();
    Map<String, Object> getSituationCredits();
    List<OperationCaisse> getHistoriqueReglementsCredit(Long venteCreditId);
    List<OperationCaisse> getOperationsDuJour();
    List<OperationCaisse> getOperationsDeLaSemaine();
    List<OperationCaisse> getOperationsDuMois();
    List<OperationCaisse> getOperationsDeLAnnee();
    List<OperationCaisse> getOperationsParPeriode(LocalDate dateDebut, LocalDate dateFin);
    Map<String, Object> getStatistiquesDuJour();
    Map<String, Object> getStatistiquesDeLaSemaine();
    Map<String, Object> getStatistiquesDuMois();
    Map<String, Object> getStatistiquesDeLAnnee();
    Map<String, Object> getStatistiquesParPeriode(LocalDate dateDebut, LocalDate dateFin);
    byte[] genererRapportJournalier(LocalDate date);
    byte[] genererRapportHebdomadaire(LocalDate debutSemaine, LocalDate finSemaine);
    byte[] genererRapportMensuel(int annee, int mois);
    byte[] genererRapportAnnuel(int annee);
    byte[] genererRapportPersonnalise(LocalDate dateDebut, LocalDate dateFin);

    // ==================== NOUVELLES MÉTHODES ====================

    OperationCaisse annulerVente(Vente vente, Long utilisateurId, String motif);
    OperationCaisse annulerVenteCredit(Vente vente, Long utilisateurId, String motif);
    Map<String, Object> getRevenusEtPertesParPeriode(LocalDate dateDebut, LocalDate dateFin);
}