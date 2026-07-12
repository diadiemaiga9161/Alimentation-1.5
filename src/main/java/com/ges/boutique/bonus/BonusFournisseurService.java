package com.ges.boutique.bonus;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.fournisseur.FournisseurRepository;
import com.ges.boutique.produit.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BonusFournisseurService {

    private final BonusFournisseurRepository bonusRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;

    @Transactional
    public BonusFournisseurDto creer(BonusFournisseurRequest req) {
        if (req.getFournisseurId() == null)
            throw new IllegalArgumentException("Le fournisseur est obligatoire");
        if (req.getType() == null)
            throw new IllegalArgumentException("Le type de bonus est obligatoire");
        if (req.getMontant() == null || req.getMontant() < 0)
            throw new IllegalArgumentException("Le montant doit être positif ou nul");

        BonusFournisseur bonus = new BonusFournisseur();
        bonus.setFournisseur(fournisseurRepository.findById(req.getFournisseurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable: " + req.getFournisseurId())));
        bonus.setType(req.getType());
        bonus.setMontant(req.getMontant() != null ? req.getMontant() : 0.0);
        bonus.setDate(req.getDate() != null ? req.getDate() : LocalDate.now());
        bonus.setDescription(req.getDescription());

        if (req.getProduitId() != null) {
            bonus.setProduit(produitRepository.findById(req.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable: " + req.getProduitId())));
            bonus.setQuantiteProduit(req.getQuantiteProduit());
        }

        return BonusFournisseurDto.from(bonusRepository.save(bonus));
    }

    @Transactional
    public BonusFournisseurDto modifier(Long id, BonusFournisseurRequest req) {
        BonusFournisseur bonus = bonusRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Bonus introuvable: " + id));

        if (req.getFournisseurId() != null)
            bonus.setFournisseur(fournisseurRepository.findById(req.getFournisseurId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable: " + req.getFournisseurId())));
        if (req.getType() != null) bonus.setType(req.getType());
        if (req.getMontant() != null) bonus.setMontant(req.getMontant());
        if (req.getDate() != null) bonus.setDate(req.getDate());
        if (req.getDescription() != null) bonus.setDescription(req.getDescription());
        if (req.getProduitId() != null) {
            bonus.setProduit(produitRepository.findById(req.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable: " + req.getProduitId())));
            bonus.setQuantiteProduit(req.getQuantiteProduit());
        }

        return BonusFournisseurDto.from(bonusRepository.save(bonus));
    }

    public void supprimer(Long id) {
        if (!bonusRepository.existsById(id))
            throw new RessourceIntrouvableException("Bonus introuvable: " + id);
        bonusRepository.deleteById(id);
    }

    public List<BonusFournisseurDto> listerTous() {
        return bonusRepository.findAllByOrderByDateDesc()
                .stream().map(BonusFournisseurDto::from).toList();
    }

    public List<BonusFournisseurDto> listerParFournisseur(Long fournisseurId) {
        return bonusRepository.findByFournisseurIdOrderByDateDesc(fournisseurId)
                .stream().map(BonusFournisseurDto::from).toList();
    }

    public List<BonusFournisseurDto> listerParPeriode(LocalDate debut, LocalDate fin) {
        return bonusRepository.findByPeriode(debut, fin)
                .stream().map(BonusFournisseurDto::from).toList();
    }

    public Map<String, Object> statistiques(int mois, int annee) {
        double totalMois = safe(bonusRepository.sumMontantByMoisAnnee(mois, annee));
        double totalAnnee = safe(bonusRepository.sumMontantByAnnee(annee));

        LocalDate premierMois = LocalDate.of(annee, mois, 1);
        LocalDate dernierMois = premierMois.withDayOfMonth(premierMois.lengthOfMonth());
        List<BonusFournisseurDto> lignesMois = listerParPeriode(premierMois, dernierMois);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mois", mois);
        r.put("annee", annee);
        r.put("totalMois", totalMois);
        r.put("totalAnnee", totalAnnee);
        r.put("nombreMois", lignesMois.size());
        r.put("lignes", lignesMois);
        return r;
    }

    private double safe(Double v) { return v != null ? v : 0.0; }
}
