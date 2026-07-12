package com.ges.boutique.employe;

import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.caisse.OperationCaisse;
import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaiementEmployeServiceImpl implements PaiementEmployeService {

    private final PaiementEmployeRepository paiementRepository;
    private final EmployeRepository employeRepository;
    private final CaisseService caisseService;

    @Override
    @Transactional
    public PaiementEmployeDto payerEmploye(PaiementEmployeRequest request) {
        if (request.getEmployeId() == null)
            throw new IllegalArgumentException("L'employé est obligatoire");
        if (request.getNombreMois() == null || request.getNombreMois() < 1 || request.getNombreMois() > 3)
            throw new IllegalArgumentException("Le nombre de mois doit être entre 1 et 3");
        if (request.getPeriodeDebut() == null || request.getPeriodeDebut().isBlank())
            throw new IllegalArgumentException("La période de début est obligatoire");

        Employe employe = employeRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable"));

        if (employe.getStatut() == StatutEmploye.INACTIF)
            throw new IllegalStateException("Impossible de payer un employé inactif");

        double montantTotal = employe.getSalaireMensuel() * request.getNombreMois();

        String motifCaisse = "Salaire employé - " + (employe.getPrenom() != null ? employe.getPrenom() + " " : "")
                + employe.getNom() + " - " + request.getPeriodeDebut()
                + (request.getNombreMois() > 1 && request.getPeriodeFin() != null
                        ? " à " + request.getPeriodeFin() : "")
                + " (" + request.getNombreMois() + " mois)";

        OperationCaisse operation = caisseService.sortieCaisseEmploye(
                montantTotal, motifCaisse, request.getUtilisateurId());

        PaiementEmploye paiement = new PaiementEmploye();
        paiement.setEmploye(employe);
        paiement.setCaisse(operation.getCaisse());
        paiement.setMontant(montantTotal);
        paiement.setNombreMois(request.getNombreMois());
        paiement.setPeriodeDebut(request.getPeriodeDebut());
        paiement.setPeriodeFin(request.getNombreMois() > 1 ? request.getPeriodeFin() : request.getPeriodeDebut());
        paiement.setStatut(StatutPaiementEmploye.PAYE);
        paiement.setUtilisateurId(request.getUtilisateurId());
        paiement.setOperationCaisseId(operation.getId());
        paiement.setObservation(request.getObservation());

        PaiementEmploye saved = paiementRepository.save(paiement);
        log.info("Salaire payé: {} F à {} ({} mois)", montantTotal, employe.getNom(), request.getNombreMois());
        return PaiementEmployeDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public PaiementEmployeDto annulerPaiement(Long paiementId, String motifAnnulation, Long utilisateurId) {
        PaiementEmploye paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Paiement introuvable avec l'id: " + paiementId));

        if (paiement.getStatut() == StatutPaiementEmploye.ANNULE)
            throw new IllegalStateException("Ce paiement est déjà annulé");

        String motifCaisse = "Annulation salaire - " + paiement.getEmploye().getNom()
                + " - " + paiement.getPeriodeDebut()
                + (motifAnnulation != null ? " : " + motifAnnulation : "");

        caisseService.retourCaisseEmploye(paiement.getMontant(), motifCaisse, utilisateurId);

        paiement.setStatut(StatutPaiementEmploye.ANNULE);
        paiement.setMotifAnnulation(motifAnnulation);
        paiement.setDateAnnulation(LocalDateTime.now());
        paiement.setUtilisateurAnnulationId(utilisateurId);

        PaiementEmploye saved = paiementRepository.save(paiement);
        log.info("Paiement id={} annulé, {} F retournés en caisse", paiementId, paiement.getMontant());
        return PaiementEmployeDto.fromEntity(saved);
    }

    @Override
    public PaiementEmployeDto getPaiementById(Long id) {
        return PaiementEmployeDto.fromEntity(paiementRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Paiement introuvable avec l'id: " + id)));
    }

    @Override
    public List<PaiementEmployeDto> getTousLesPaiements() {
        return paiementRepository.findAllByOrderByDatePaiementDesc().stream()
                .map(PaiementEmployeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaiementEmployeDto> getPaiementsParEmploye(Long employeId) {
        return paiementRepository.findByEmployeIdOrderByDatePaiementDesc(employeId).stream()
                .map(PaiementEmployeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaiementEmployeDto> getPaiementsActifs() {
        return paiementRepository.findByStatutOrderByDatePaiementDesc(StatutPaiementEmploye.PAYE).stream()
                .map(PaiementEmployeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getStatistiques() {
        List<PaiementEmploye> tous = paiementRepository.findAllByOrderByDatePaiementDesc();
        long totalPaie = tous.stream().filter(p -> p.getStatut() == StatutPaiementEmploye.PAYE).count();
        long totalAnnule = tous.stream().filter(p -> p.getStatut() == StatutPaiementEmploye.ANNULE).count();
        Double montantTotal = tous.stream()
                .filter(p -> p.getStatut() == StatutPaiementEmploye.PAYE)
                .mapToDouble(PaiementEmploye::getMontant).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPaiements", tous.size());
        stats.put("totalPaies", totalPaie);
        stats.put("totalAnnules", totalAnnule);
        stats.put("montantTotalPaye", montantTotal);
        return stats;
    }
}
