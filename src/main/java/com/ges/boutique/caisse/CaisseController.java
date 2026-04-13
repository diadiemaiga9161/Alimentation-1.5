package com.ges.boutique.caisse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caisse")
@RequiredArgsConstructor
@Tag(name = "Caisse", description = "Gestion complète de la caisse")
public class CaisseController {

    private final CaisseService caisseService;

    @PostMapping("/ouvrir")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ouvrir la caisse")
    public ResponseEntity<Map<String, Object>> ouvrirCaisse() {
        Caisse caisse = caisseService.ouvrirCaisse();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Caisse ouverte avec succès");
        response.put("caisse", caisse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fermer")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fermer la caisse")
    public ResponseEntity<Map<String, Object>> fermerCaisse(@RequestParam(required = false) Long utilisateurId) {
        Caisse caisse = caisseService.fermerCaisse(utilisateurId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Caisse fermée avec succès");
        response.put("caisse", caisse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verifier")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Vérifier la caisse avec solde réel")
    public ResponseEntity<Map<String, Object>> verifierCaisse(@RequestBody VerificationCaisseRequest request) {
        Caisse caisse = caisseService.verifierCaisse(
                request.getSoldeReelSaisi(),
                request.getUtilisateurId(),
                request.getObservations()
        );
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Caisse vérifiée avec succès");
        response.put("caisse", caisse);
        response.put("ecart", caisse.getEcart());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/etat")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir l'état de la caisse")
    public ResponseEntity<Map<String, Object>> getEtatCaisse() {
        Caisse caisse = caisseService.getCaisseOuverte();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("caisse", caisse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/solde")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir le solde actuel de la caisse")
    public ResponseEntity<Map<String, Object>> getSolde() {
        Double solde = caisseService.getSoldeActuel();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("solde", solde);
        response.put("soldeSysteme", caisseService.getSoldeSysteme());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ecart")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir l'écart de caisse")
    public ResponseEntity<Map<String, Object>> getEcart() {
        Map<String, Object> ecart = caisseService.getEcartCaisse();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("ecart", ecart);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/entree")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajouter une entrée en caisse")
    public ResponseEntity<Map<String, Object>> entreeCaisse(@RequestBody CaisseRequest request) {
        OperationCaisse operation = caisseService.entreeCaisse(
                request.getMontant(),
                request.getMotif(),
                request.getUtilisateurId(),
                request.getModePaiement(),
                request.getReferencePaiement()
        );
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Entrée en caisse enregistrée avec succès");
        response.put("operation", operation);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sortie")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajouter une sortie de caisse")
    public ResponseEntity<Map<String, Object>> sortieCaisse(@RequestBody CaisseRequest request) {
        OperationCaisse operation = caisseService.sortieCaisse(
                request.getMontant(),
                request.getMotif(),
                request.getUtilisateurId()
        );
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sortie de caisse enregistrée avec succès");
        response.put("operation", operation);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir tous les crédits non réglés")
    public ResponseEntity<Map<String, Object>> getCreditsNonRegles() {
        List<OperationCaisse> credits = caisseService.getCreditsNonRegles();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("credits", credits);
        response.put("nombreCredits", credits.size());
        response.put("montantTotal", credits.stream().mapToDouble(OperationCaisse::getMontantRestant).sum());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits/retard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les crédits en retard")
    public ResponseEntity<Map<String, Object>> getCreditsEnRetard() {
        List<OperationCaisse> credits = caisseService.getCreditsEnRetard();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("creditsEnRetard", credits);
        response.put("nombreCreditsEnRetard", credits.size());
        response.put("montantTotalRetard", credits.stream().mapToDouble(OperationCaisse::getMontantRestant).sum());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/credits/reglement")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Enregistrer le règlement d'un crédit")
    public ResponseEntity<Map<String, Object>> reglementCredit(@RequestBody ReglementCreditRequest request) {
        OperationCaisse reglement = caisseService.reglementCredit(
                request.getVenteCreditId(),
                request.getMontantRegle(),
                request.getUtilisateurId(),
                request.getModePaiement(),
                request.getReferencePaiement()
        );
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Règlement de crédit enregistré avec succès");
        response.put("reglement", reglement);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits/situation")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir la situation complète des crédits")
    public ResponseEntity<Map<String, Object>> getSituationCredits() {
        Map<String, Object> situation = caisseService.getSituationCredits();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("situation", situation);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits/{venteCreditId}/reglements")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir l'historique des règlements d'un crédit")
    public ResponseEntity<Map<String, Object>> getHistoriqueReglementsCredit(@PathVariable Long venteCreditId) {
        List<OperationCaisse> reglements = caisseService.getHistoriqueReglementsCredit(venteCreditId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("reglements", reglements);
        response.put("nombreReglements", reglements.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operations/aujourdhui")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les opérations du jour")
    public ResponseEntity<Map<String, Object>> getOperationsDuJour() {
        List<OperationCaisse> operations = caisseService.getOperationsDuJour();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("operations", operations);
        response.put("nombreOperations", operations.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operations/semaine")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les opérations de la semaine")
    public ResponseEntity<Map<String, Object>> getOperationsDeLaSemaine() {
        List<OperationCaisse> operations = caisseService.getOperationsDeLaSemaine();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("operations", operations);
        response.put("nombreOperations", operations.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operations/mois")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les opérations du mois")
    public ResponseEntity<Map<String, Object>> getOperationsDuMois() {
        List<OperationCaisse> operations = caisseService.getOperationsDuMois();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("operations", operations);
        response.put("nombreOperations", operations.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operations/annee")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les opérations de l'année")
    public ResponseEntity<Map<String, Object>> getOperationsDeLAnnee() {
        List<OperationCaisse> operations = caisseService.getOperationsDeLAnnee();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("operations", operations);
        response.put("nombreOperations", operations.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operations/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les opérations sur une période personnalisée")
    public ResponseEntity<Map<String, Object>> getOperationsParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        List<OperationCaisse> operations = caisseService.getOperationsParPeriode(dateDebut, dateFin);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("operations", operations);
        response.put("nombreOperations", operations.size());
        response.put("dateDebut", dateDebut);
        response.put("dateFin", dateFin);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques/aujourdhui")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les statistiques du jour")
    public ResponseEntity<Map<String, Object>> getStatistiquesDuJour() {
        Map<String, Object> statistiques = caisseService.getStatistiquesDuJour();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("date", LocalDate.now());
        response.put("statistiques", statistiques);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques/semaine")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques de la semaine")
    public ResponseEntity<Map<String, Object>> getStatistiquesDeLaSemaine() {
        Map<String, Object> statistiques = caisseService.getStatistiquesDeLaSemaine();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("statistiques", statistiques);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques/mois")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques du mois")
    public ResponseEntity<Map<String, Object>> getStatistiquesDuMois() {
        Map<String, Object> statistiques = caisseService.getStatistiquesDuMois();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("statistiques", statistiques);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques/annee")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques de l'année")
    public ResponseEntity<Map<String, Object>> getStatistiquesDeLAnnee() {
        Map<String, Object> statistiques = caisseService.getStatistiquesDeLAnnee();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("statistiques", statistiques);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques/periode")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques sur une période personnalisée")
    public ResponseEntity<Map<String, Object>> getStatistiquesParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        Map<String, Object> statistiques = caisseService.getStatistiquesParPeriode(dateDebut, dateFin);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("statistiques", statistiques);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rapports/journalier")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer le rapport journalier PDF")
    public ResponseEntity<byte[]> genererRapportJournalier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate dateRapport = date != null ? date : LocalDate.now();
        byte[] rapport = caisseService.genererRapportJournalier(dateRapport);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "rapport-caisse-" + dateRapport + ".pdf");
        return ResponseEntity.ok().headers(headers).body(rapport);
    }

    @GetMapping("/rapports/hebdomadaire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer le rapport hebdomadaire PDF")
    public ResponseEntity<byte[]> genererRapportHebdomadaire(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debutSemaine,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate finSemaine) {
        LocalDate debut = debutSemaine != null ? debutSemaine : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate fin = finSemaine != null ? finSemaine : debut.plusDays(6);
        byte[] rapport = caisseService.genererRapportHebdomadaire(debut, fin);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "rapport-caisse-semaine-" + debut + "-" + fin + ".pdf");
        return ResponseEntity.ok().headers(headers).body(rapport);
    }

    @GetMapping("/rapports/mensuel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer le rapport mensuel PDF")
    public ResponseEntity<byte[]> genererRapportMensuel(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois) {
        int anneeRapport = annee != null ? annee : LocalDate.now().getYear();
        int moisRapport = mois != null ? mois : LocalDate.now().getMonthValue();
        byte[] rapport = caisseService.genererRapportMensuel(anneeRapport, moisRapport);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "rapport-caisse-" + anneeRapport + "-" + moisRapport + ".pdf");
        return ResponseEntity.ok().headers(headers).body(rapport);
    }

    @GetMapping("/rapports/annuel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer le rapport annuel PDF")
    public ResponseEntity<byte[]> genererRapportAnnuel(
            @RequestParam(required = false) Integer annee) {
        int anneeRapport = annee != null ? annee : LocalDate.now().getYear();
        byte[] rapport = caisseService.genererRapportAnnuel(anneeRapport);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "rapport-caisse-" + anneeRapport + ".pdf");
        return ResponseEntity.ok().headers(headers).body(rapport);
    }

    @GetMapping("/rapports/personnalise")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer un rapport personnalisé PDF")
    public ResponseEntity<byte[]> genererRapportPersonnalise(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        byte[] rapport = caisseService.genererRapportPersonnalise(dateDebut, dateFin);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "rapport-caisse-" + dateDebut + "-" + dateFin + ".pdf");
        return ResponseEntity.ok().headers(headers).body(rapport);
    }

    @GetMapping("/revenus-pertes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les revenus et pertes sur une période")
    public ResponseEntity<Map<String, Object>> getRevenusEtPertesParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        Map<String, Object> resultats = caisseService.getRevenusEtPertesParPeriode(dateDebut, dateFin);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("resultats", resultats);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenus-pertes/semaine")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les revenus et pertes de la semaine")
    public ResponseEntity<Map<String, Object>> getRevenusEtPertesSemaine() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutSemaine = aujourdhui.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate finSemaine = aujourdhui.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        Map<String, Object> resultats = caisseService.getRevenusEtPertesParPeriode(debutSemaine, finSemaine);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("resultats", resultats);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenus-pertes/mois")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les revenus et pertes du mois")
    public ResponseEntity<Map<String, Object>> getRevenusEtPertesMois() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);
        LocalDate finMois = aujourdhui.withDayOfMonth(aujourdhui.lengthOfMonth());

        Map<String, Object> resultats = caisseService.getRevenusEtPertesParPeriode(debutMois, finMois);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("resultats", resultats);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenus-pertes/annee")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les revenus et pertes de l'année")
    public ResponseEntity<Map<String, Object>> getRevenusEtPertesAnnee() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutAnnee = aujourdhui.withDayOfYear(1);
        LocalDate finAnnee = aujourdhui.withDayOfYear(aujourdhui.lengthOfYear());

        Map<String, Object> resultats = caisseService.getRevenusEtPertesParPeriode(debutAnnee, finAnnee);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("resultats", resultats);

        return ResponseEntity.ok(response);
    }
}