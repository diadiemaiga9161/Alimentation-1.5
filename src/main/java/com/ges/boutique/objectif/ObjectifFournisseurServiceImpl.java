package com.ges.boutique.objectif;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.fournisseur.Fournisseur;
import com.ges.boutique.fournisseur.FournisseurRepository;
import com.ges.boutique.inventaire.InventaireService;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ObjectifFournisseurServiceImpl implements ObjectifFournisseurService {

    private final ObjectifFournisseurRepository repository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final InventaireService inventaireService;

    @Override
    @Transactional
    public ObjectifFournisseurDto creer(ObjectifFournisseurRequest request) {
        validate(request);
        Fournisseur fournisseur = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable"));

        ObjectifFournisseur objectif = new ObjectifFournisseur();
        objectif.setFournisseur(fournisseur);
        objectif.setMois(request.getMois());
        objectif.setAnnee(request.getAnnee());
        objectif.setObjectifQuantite(request.getObjectifQuantite());
        objectif.setBonusParUnite(request.getBonusParUnite());
        objectif.setObservation(request.getObservation());

        if (request.getProduitId() != null) {
            Produit produit = produitRepository.findById(request.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable"));
            objectif.setProduit(produit);
        }

        double qteAtteinte = request.getQuantiteAtteinte() != null ? request.getQuantiteAtteinte() : 0.0;
        double qteBonusRecue = request.getQuantiteBonusRecue() != null ? request.getQuantiteBonusRecue() : 0.0;
        objectif.setQuantiteAtteinte(qteAtteinte);
        objectif.setQuantiteBonusRecue(qteBonusRecue);
        objectif.setBonusCalcule(qteAtteinte * request.getBonusParUnite());
        objectif.setStatut(qteAtteinte >= request.getObjectifQuantite() ? StatutObjectif.ATTEINT : StatutObjectif.NON_ATTEINT);

        return ObjectifFournisseurDto.fromEntity(repository.save(objectif));
    }

    @Override
    @Transactional
    public ObjectifFournisseurDto modifier(Long id, ObjectifFournisseurRequest request) {
        ObjectifFournisseur objectif = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Objectif introuvable"));

        if (Boolean.TRUE.equals(objectif.getStockAjoute())) {
            throw new IllegalStateException("Cet objectif a déjà été validé et le stock ajouté. Impossible de modifier.");
        }

        validate(request);
        Fournisseur fournisseur = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable"));

        objectif.setFournisseur(fournisseur);
        objectif.setMois(request.getMois());
        objectif.setAnnee(request.getAnnee());
        objectif.setObjectifQuantite(request.getObjectifQuantite());
        objectif.setBonusParUnite(request.getBonusParUnite());
        objectif.setObservation(request.getObservation());

        if (request.getProduitId() != null) {
            Produit produit = produitRepository.findById(request.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable"));
            objectif.setProduit(produit);
        } else {
            objectif.setProduit(null);
        }

        double qteAtteinte = request.getQuantiteAtteinte() != null ? request.getQuantiteAtteinte() : 0.0;
        double qteBonusRecue = request.getQuantiteBonusRecue() != null ? request.getQuantiteBonusRecue() : 0.0;
        objectif.setQuantiteAtteinte(qteAtteinte);
        objectif.setQuantiteBonusRecue(qteBonusRecue);
        objectif.setBonusCalcule(qteAtteinte * request.getBonusParUnite());
        objectif.setStatut(qteAtteinte >= request.getObjectifQuantite() ? StatutObjectif.ATTEINT : StatutObjectif.NON_ATTEINT);

        return ObjectifFournisseurDto.fromEntity(repository.save(objectif));
    }

    @Override
    public ObjectifFournisseurDto getById(Long id) {
        return ObjectifFournisseurDto.fromEntity(
                repository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Objectif introuvable")));
    }

    @Override
    public List<ObjectifFournisseurDto> getTous() {
        return repository.findAllByOrderByAnneeDescMoisDescDateCreationDesc()
                .stream().map(ObjectifFournisseurDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<ObjectifFournisseurDto> getParMoisAnnee(int mois, int annee) {
        return repository.findByMoisAndAnneeOrderByDateCreationDesc(mois, annee)
                .stream().map(ObjectifFournisseurDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<ObjectifFournisseurDto> getParFournisseur(Long fournisseurId) {
        return repository.findByFournisseurIdOrderByAnneeDescMoisDesc(fournisseurId)
                .stream().map(ObjectifFournisseurDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<ObjectifFournisseurDto> getParAnnee(int annee) {
        return repository.findByAnneeOrderByMoisDescDateCreationDesc(annee)
                .stream().map(ObjectifFournisseurDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ObjectifFournisseurDto valider(Long id) {
        ObjectifFournisseur objectif = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Objectif introuvable"));

        if (Boolean.TRUE.equals(objectif.getStockAjoute())) {
            throw new IllegalStateException("Le stock a déjà été ajouté pour cet objectif.");
        }
        if (objectif.getStatut() != StatutObjectif.ATTEINT) {
            throw new IllegalStateException("Seuls les objectifs atteints peuvent être validés.");
        }
        if (objectif.getProduit() == null) {
            throw new IllegalStateException("Un produit doit être lié à l'objectif pour ajouter au stock.");
        }
        if (objectif.getQuantiteBonusRecue() == null || objectif.getQuantiteBonusRecue() <= 0) {
            throw new IllegalArgumentException("La quantité bonus reçue doit être supérieure à 0.");
        }

        inventaireService.entreeStockBonusFournisseur(
                objectif.getProduit().getId(),
                objectif.getQuantiteBonusRecue().intValue(),
                objectif.getId()
        );

        objectif.setStockAjoute(true);
        objectif.setDateValidation(LocalDateTime.now());

        return ObjectifFournisseurDto.fromEntity(repository.save(objectif));
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        ObjectifFournisseur objectif = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Objectif introuvable"));
        if (Boolean.TRUE.equals(objectif.getStockAjoute())) {
            throw new IllegalStateException("Impossible de supprimer un objectif dont le stock a déjà été ajouté.");
        }
        repository.delete(objectif);
    }

    @Override
    public StatsObjectifDto getStatistiques(int mois, int annee) {
        long total = repository.countByMoisAnnee(mois, annee);
        long atteints = repository.countAtteintsByMoisAnnee(mois, annee);
        double totalBonus = repository.getTotalBonusParMoisAnnee(mois, annee);
        double totalQtBonus = repository.getTotalQuantiteBonusRecueParMoisAnnee(mois, annee);
        return new StatsObjectifDto(mois, annee, total, atteints, total - atteints, totalBonus, totalQtBonus);
    }

    @Override
    public Map<String, Object> getRapportMensuel(int mois, int annee) {
        Map<String, Object> rapport = new HashMap<>();
        List<ObjectifFournisseurDto> objectifs = getParMoisAnnee(mois, annee);
        StatsObjectifDto stats = getStatistiques(mois, annee);

        rapport.put("mois", mois);
        rapport.put("annee", annee);
        rapport.put("objectifs", objectifs);
        rapport.put("statistiques", stats);
        rapport.put("totalBonusFournisseur", stats.getTotalBonusCalcule());
        rapport.put("totalQuantiteBonusRecue", stats.getTotalQuantiteBonusRecue());
        rapport.put("tauxAtteinte", stats.getTotalObjectifs() > 0
                ? (stats.getObjectifsAtteints() * 100.0 / stats.getTotalObjectifs()) : 0.0);
        return rapport;
    }

    @Override
    public Map<String, Object> getRapportAnnuel(int annee) {
        Map<String, Object> rapport = new HashMap<>();
        List<ObjectifFournisseurDto> objectifs = getParAnnee(annee);
        double totalBonus = repository.getTotalBonusParAnnee(annee);

        rapport.put("annee", annee);
        rapport.put("objectifs", objectifs);
        rapport.put("totalBonusFournisseur", totalBonus);
        rapport.put("totalObjectifs", objectifs.size());
        rapport.put("objectifsAtteints", objectifs.stream().filter(o -> o.getStatut() == StatutObjectif.ATTEINT).count());
        rapport.put("totalQuantiteBonusRecue", objectifs.stream()
                .mapToDouble(o -> o.getQuantiteBonusRecue() != null ? o.getQuantiteBonusRecue() : 0).sum());
        return rapport;
    }

    private void validate(ObjectifFournisseurRequest r) {
        if (r.getFournisseurId() == null) throw new IllegalArgumentException("Le fournisseur est obligatoire.");
        if (r.getMois() == null || r.getMois() < 1 || r.getMois() > 12)
            throw new IllegalArgumentException("Le mois doit être entre 1 et 12.");
        if (r.getAnnee() == null || r.getAnnee() < 2000)
            throw new IllegalArgumentException("L'année est invalide.");
        if (r.getObjectifQuantite() == null || r.getObjectifQuantite() <= 0)
            throw new IllegalArgumentException("L'objectif de quantité doit être supérieur à 0.");
        if (r.getBonusParUnite() == null || r.getBonusParUnite() < 0)
            throw new IllegalArgumentException("Le bonus par unité ne peut pas être négatif.");
    }
}
