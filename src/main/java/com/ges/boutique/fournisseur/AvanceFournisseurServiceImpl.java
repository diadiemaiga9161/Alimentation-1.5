package com.ges.boutique.fournisseur;

import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.compte.CompteService;
import com.ges.boutique.compte.TypeOperationCompte;
import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvanceFournisseurServiceImpl implements AvanceFournisseurService {

    private final AvanceFournisseurRepository avanceRepository;
    private final FournisseurRepository fournisseurRepository;
    private final CaisseService caisseService;
    private final CompteService compteService;

    @Override
    @Transactional
    public AvanceFournisseur enregistrerAvance(AvanceFournisseurRequest request) {
        if (request.getMontant() == null || request.getMontant() <= 0)
            throw new IllegalArgumentException("Le montant de l'avance doit être supérieur à 0");
        if (request.getFournisseurId() == null)
            throw new IllegalArgumentException("Le fournisseur est obligatoire");
        if (request.getSourceFinancement() == null)
            throw new IllegalArgumentException("La source de financement est obligatoire (CAISSE ou BANQUE)");

        Fournisseur fournisseur = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable"));

        String motifOp = "Avance fournisseur - " + fournisseur.getNom()
                + (request.getMotif() != null ? " : " + request.getMotif() : "");

        AvanceFournisseur avance = new AvanceFournisseur();
        avance.setFournisseur(fournisseur);
        avance.setMontant(request.getMontant());
        avance.setMontantUtilise(0.0);
        avance.setMontantDisponible(request.getMontant());
        avance.setMotif(request.getMotif());
        avance.setSourceFinancement(request.getSourceFinancement().toUpperCase());
        avance.setUtilisateurId(request.getUtilisateurId());

        if ("CAISSE".equalsIgnoreCase(request.getSourceFinancement())) {
            var op = caisseService.sortieCaisseAvance(request.getMontant(), motifOp, request.getUtilisateurId());
            avance.setReferenceCaisseOperation(op.getId());
        } else if ("BANQUE".equalsIgnoreCase(request.getSourceFinancement())) {
            if (request.getCompteId() == null)
                throw new IllegalArgumentException("Le compte bancaire est obligatoire pour une avance depuis la banque");
            avance.setCompteId(request.getCompteId());
            compteService.debiterCompte(request.getCompteId(), request.getMontant(), motifOp,
                    TypeOperationCompte.AVANCE_FOURNISSEUR.toString(), request.getUtilisateurId());
        } else {
            throw new IllegalArgumentException("Source de financement invalide : " + request.getSourceFinancement());
        }

        AvanceFournisseur saved = avanceRepository.save(avance);
        log.info("Avance fournisseur enregistrée: {} F pour {} depuis {}", request.getMontant(), fournisseur.getNom(), request.getSourceFinancement());
        return saved;
    }

    @Override
    public Double getSoldeDisponible(Long fournisseurId) {
        if (fournisseurId == null) return 0.0;
        return avanceRepository.getSoldeDisponibleByFournisseurId(fournisseurId);
    }

    @Override
    public List<AvanceFournisseur> getHistoriqueParFournisseur(Long fournisseurId) {
        return avanceRepository.findByFournisseurIdOrderByDateDesc(fournisseurId);
    }

    @Override
    public List<AvanceFournisseur> getToutesLesAvances() {
        return avanceRepository.findAllOrderByDateDesc();
    }

    @Override
    @Transactional
    public void utiliserAvance(Long fournisseurId, Double montantAUtiliser) {
        if (montantAUtiliser == null || montantAUtiliser <= 0) return;

        Double solde = getSoldeDisponible(fournisseurId);
        if (solde < montantAUtiliser - 0.01) {
            throw new IllegalStateException(
                    "Solde avance fournisseur insuffisant. Disponible: " + solde + ", Demandé: " + montantAUtiliser);
        }

        List<AvanceFournisseur> avances = avanceRepository.findAvancesDisponiblesByFournisseurId(fournisseurId);
        double reste = montantAUtiliser;

        for (AvanceFournisseur avance : avances) {
            if (reste <= 0.01) break;
            double utilise = Math.min(avance.getMontantDisponible(), reste);
            avance.setMontantUtilise(avance.getMontantUtilise() + utilise);
            avance.setMontantDisponible(avance.getMontantDisponible() - utilise);
            reste -= utilise;
            avanceRepository.save(avance);
        }

        log.info("Avance fournisseur utilisée: {} F pour fournisseur id={}", montantAUtiliser, fournisseurId);
    }

    @Override
    @Transactional
    public void annulerUtilisationAvance(Long fournisseurId, Double montantAAnnuler) {
        if (montantAAnnuler == null || montantAAnnuler <= 0) {
            log.info("Aucun montant à annuler pour l'avance du fournisseur {}", fournisseurId);
            return;
        }

        log.info("=== ANNULATION UTILISATION AVANCE ===");
        log.info("Fournisseur ID: {}, Montant à annuler: {} F", fournisseurId, montantAAnnuler);

        List<AvanceFournisseur> avances = avanceRepository.findByFournisseurIdOrderByDateDesc(fournisseurId);
        double resteARembourser = montantAAnnuler;

        for (AvanceFournisseur avance : avances) {
            if (resteARembourser <= 0.01) break;

            double utiliseSurCetteAvance = avance.getMontantUtilise();
            if (utiliseSurCetteAvance <= 0) continue;

            double aEnlever = Math.min(utiliseSurCetteAvance, resteARembourser);

            avance.setMontantUtilise(avance.getMontantUtilise() - aEnlever);
            avance.setMontantDisponible(avance.getMontantDisponible() + aEnlever);
            resteARembourser -= aEnlever;

            avanceRepository.save(avance);

            log.info("✓ Avance #{}: {} F annulés (utilisé: {} → {}, disponible: {} → {})",
                    avance.getId(), aEnlever,
                    utiliseSurCetteAvance, avance.getMontantUtilise(),
                    avance.getMontantDisponible() - aEnlever, avance.getMontantDisponible());
        }

        if (resteARembourser > 0.01) {
            log.warn("⚠️ Attention: {} F n'ont pas pu être annulés (pas assez d'avance utilisée)", resteARembourser);
        } else {
            log.info("✅ Annulation terminée: {} F remboursés dans les avances", montantAAnnuler - resteARembourser);
        }
    }
}