package com.ges.boutique.depense;

import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DepenseServiceImpl implements DepenseService {

    private final DepenseRepository depenseRepository;
    private final CaisseService caisseService;

    @Override
    @Transactional
    public Depense creerDepense(DepenseRequest request, Long utilisateurId) {
        if (request.getNom() == null || request.getNom().isBlank())
            throw new IllegalArgumentException("Le nom de la dépense est obligatoire");
        if (request.getMontant() == null || request.getMontant() <= 0)
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");

        // Sortie automatique de la caisse
        var operation = caisseService.sortieCaisseDepense(
                request.getMontant(),
                "Dépense: " + request.getNom() + (request.getMotif() != null ? " - " + request.getMotif() : ""),
                utilisateurId
        );

        Depense depense = new Depense();
        depense.setNom(request.getNom());
        depense.setMotif(request.getMotif());
        depense.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        depense.setMontant(request.getMontant());
        depense.setTypeDepense(request.getTypeDepense());
        depense.setOperationCaisseId(operation.getId());

        return depenseRepository.save(depense);
    }

    @Override
    @Transactional
    public Depense validerDepense(Long id, Long utilisateurId) {
        Depense depense = depenseRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dépense non trouvée: " + id));
        if (depense.isAnnulee())
            throw new IllegalStateException("Cette dépense est annulée, elle ne peut pas être validée");
        if (depense.isValidee())
            throw new IllegalStateException("Cette dépense est déjà validée");
        depense.setValidee(true);
        depense.setValideParId(utilisateurId);
        depense.setValideLe(java.time.LocalDateTime.now());
        return depenseRepository.save(depense);
    }

    @Override
    @Transactional
    public Depense modifierDepense(Long id, DepenseRequest request, Long utilisateurId) {
        Depense depense = depenseRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dépense non trouvée: " + id));
        if (depense.isValidee())
            throw new IllegalStateException("Cette dépense est validée, elle ne peut plus être modifiée (annulez-la si besoin)");
        if (depense.isAnnulee())
            throw new IllegalStateException("Cette dépense est annulée, elle ne peut plus être modifiée");

        double ancienMontant = depense.getMontant();
        double nouveauMontant = request.getMontant() != null ? request.getMontant() : ancienMontant;

        // Ajustement caisse si le montant change
        if (nouveauMontant != ancienMontant) {
            double difference = nouveauMontant - ancienMontant;
            String motifAjustement = "Modification dépense: " + (request.getNom() != null ? request.getNom() : depense.getNom());

            if (difference > 0) {
                // Montant augmenté → sortie supplémentaire
                caisseService.sortieCaisseDepense(difference, motifAjustement, utilisateurId);
            } else {
                // Montant diminué → retour en caisse
                caisseService.entreeCaisseDepense(-difference, motifAjustement, utilisateurId);
            }
            depense.setMontant(nouveauMontant);
        }

        if (request.getNom() != null) depense.setNom(request.getNom());
        if (request.getMotif() != null) depense.setMotif(request.getMotif());
        if (request.getDate() != null) depense.setDate(request.getDate());
        if (request.getTypeDepense() != null) depense.setTypeDepense(request.getTypeDepense());

        return depenseRepository.save(depense);
    }

    @Override
    @Transactional
    public void supprimerDepense(Long id, Long utilisateurId, String motif) {
        Depense depense = depenseRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dépense non trouvée: " + id));
        if (depense.isAnnulee())
            throw new IllegalStateException("Cette dépense est déjà annulée");

        // Retour du montant en caisse (dans les deux cas : suppression directe ou annulation tracée)
        caisseService.entreeCaisseDepense(
                depense.getMontant(),
                "Annulation dépense: " + depense.getNom(),
                utilisateurId
        );

        if (depense.isValidee()) {
            // Dépense verrouillée dans les comptes : on ne supprime jamais la ligne,
            // on la marque annulée avec la trace de qui/quand/pourquoi (même logique
            // que l'annulation d'une vente) — le montant reste exclu des totaux
            // (voir DepenseRepository, filtres "d.annulee = false") mais reste visible
            // dans l'historique pour la comptabilité.
            depense.setAnnulee(true);
            depense.setMotifAnnulation(motif != null && !motif.isBlank() ? motif : "Annulation dépense");
            depense.setAnnuleParId(utilisateurId);
            depense.setDateAnnulation(java.time.LocalDateTime.now());
            depenseRepository.save(depense);
        } else {
            // Pas encore validée → simple erreur de saisie, suppression directe autorisée
            depenseRepository.delete(depense);
        }
    }

    @Override
    public Depense obtenirParId(Long id) {
        return depenseRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dépense non trouvée: " + id));
    }

    @Override
    public List<Depense> obtenirToutes() {
        return depenseRepository.findAllByOrderByDateDesc();
    }

    @Override
    public List<Depense> obtenirParPeriode(LocalDate debut, LocalDate fin) {
        return depenseRepository.findByDateBetweenOrderByDateDesc(debut, fin);
    }

    @Override
    public Double getTotalDepenses() {
        return depenseRepository.findAll().stream()
                .filter(d -> !d.isAnnulee())
                .mapToDouble(Depense::getMontant).sum();
    }

    @Override
    public Double getTotalDepensesParPeriode(LocalDate debut, LocalDate fin) {
        return depenseRepository.findByDateBetweenOrderByDateDesc(debut, fin).stream()
                .filter(d -> !d.isAnnulee())
                .mapToDouble(Depense::getMontant).sum();
    }

    @Override
    public Map<String, Double> getTotauxParType() {
        Map<String, Double> result = new LinkedHashMap<>();
        depenseRepository.getTotauxParType().forEach(row ->
            result.put((String) row[0], ((Number) row[1]).doubleValue()));
        return result;
    }

    @Override
    public Map<String, Double> getTotauxParTypePeriode(LocalDate debut, LocalDate fin) {
        Map<String, Double> result = new LinkedHashMap<>();
        depenseRepository.getTotauxParTypePeriode(debut, fin).forEach(row ->
            result.put((String) row[0], ((Number) row[1]).doubleValue()));
        return result;
    }
}
