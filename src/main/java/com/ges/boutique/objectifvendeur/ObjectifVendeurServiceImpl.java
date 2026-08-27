package com.ges.boutique.objectifvendeur;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.objectif.StatutObjectif;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ObjectifVendeurServiceImpl implements ObjectifVendeurService {

    private final ObjectifVendeurRepository repository;
    private final UtilisateurRepository utilisateurRepository;
    private final VenteRepository venteRepository;

    @Override
    @Transactional
    public ObjectifVendeurDto creer(ObjectifVendeurRequest request) {
        validate(request);
        Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Vendeur introuvable"));

        ObjectifVendeur objectif = new ObjectifVendeur();
        objectif.setVendeur(vendeur);
        objectif.setSemaine(request.getSemaine());
        objectif.setAnnee(request.getAnnee());
        objectif.setObjectifNombreVentes(request.getObjectifNombreVentes());
        objectif.setBonusMontant(request.getBonusMontant());
        objectif.setObservation(request.getObservation());

        return toDto(repository.save(objectif));
    }

    @Override
    @Transactional
    public ObjectifVendeurDto modifier(Long id, ObjectifVendeurRequest request) {
        ObjectifVendeur objectif = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Objectif introuvable"));

        if (Boolean.TRUE.equals(objectif.getBonusValide())) {
            throw new IllegalStateException("Cet objectif a déjà été validé. Impossible de modifier.");
        }

        validate(request);
        Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Vendeur introuvable"));

        objectif.setVendeur(vendeur);
        objectif.setSemaine(request.getSemaine());
        objectif.setAnnee(request.getAnnee());
        objectif.setObjectifNombreVentes(request.getObjectifNombreVentes());
        objectif.setBonusMontant(request.getBonusMontant());
        objectif.setObservation(request.getObservation());

        return toDto(repository.save(objectif));
    }

    @Override
    public ObjectifVendeurDto getById(Long id) {
        ObjectifVendeur objectif = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Objectif introuvable"));
        return toDto(objectif);
    }

    @Override
    public List<ObjectifVendeurDto> getTous() {
        return repository.findAllByOrderByAnneeDescSemaineDescDateCreationDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ObjectifVendeurDto> getParSemaineAnnee(int semaine, int annee) {
        return repository.findBySemaineAndAnneeOrderByDateCreationDesc(semaine, annee)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ObjectifVendeurDto> getParVendeur(Long vendeurId) {
        return repository.findByVendeurIdOrderByAnneeDescSemaineDesc(vendeurId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ObjectifVendeurDto> getParAnnee(int annee) {
        return repository.findByAnneeOrderBySemaineDescDateCreationDesc(annee)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ObjectifVendeurDto valider(Long id) {
        ObjectifVendeur objectif = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Objectif introuvable"));

        if (Boolean.TRUE.equals(objectif.getBonusValide())) {
            throw new IllegalStateException("Ce bonus a déjà été validé.");
        }

        long nombreVentesAtteint = calculerNombreVentesAtteint(objectif);
        StatutObjectif statut = calculerStatut(nombreVentesAtteint, objectif.getObjectifNombreVentes());
        if (statut != StatutObjectif.ATTEINT) {
            throw new IllegalStateException("Seuls les objectifs atteints peuvent être validés.");
        }

        objectif.setBonusValide(true);
        objectif.setDateValidation(LocalDateTime.now());

        return toDto(repository.save(objectif));
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        ObjectifVendeur objectif = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Objectif introuvable"));
        if (Boolean.TRUE.equals(objectif.getBonusValide())) {
            throw new IllegalStateException("Impossible de supprimer un objectif dont le bonus a déjà été validé.");
        }
        repository.delete(objectif);
    }

    private ObjectifVendeurDto toDto(ObjectifVendeur e) {
        ObjectifVendeurDto dto = new ObjectifVendeurDto();
        dto.setId(e.getId());
        dto.setVendeurId(e.getVendeur().getId());
        dto.setVendeurNom(e.getVendeur().getNomComplet());
        dto.setSemaine(e.getSemaine());
        dto.setAnnee(e.getAnnee());
        dto.setObjectifNombreVentes(e.getObjectifNombreVentes());
        dto.setBonusMontant(e.getBonusMontant());

        long nombreVentesAtteint = calculerNombreVentesAtteint(e);
        dto.setNombreVentesAtteint(nombreVentesAtteint);
        dto.setStatut(calculerStatut(nombreVentesAtteint, e.getObjectifNombreVentes()));

        dto.setBonusValide(e.getBonusValide());
        dto.setObservation(e.getObservation());
        dto.setDateValidation(e.getDateValidation());
        dto.setDateCreation(e.getDateCreation());
        return dto;
    }

    private long calculerNombreVentesAtteint(ObjectifVendeur objectif) {
        LocalDateTime[] plage = getPlageSemaine(objectif.getSemaine(), objectif.getAnnee());
        Long count = venteRepository.countByVendeurIdAndDateRange(
                objectif.getVendeur().getId(), plage[0], plage[1]);
        return count != null ? count : 0L;
    }

    private StatutObjectif calculerStatut(long nombreVentesAtteint, Integer objectifNombreVentes) {
        return nombreVentesAtteint >= objectifNombreVentes ? StatutObjectif.ATTEINT : StatutObjectif.NON_ATTEINT;
    }

    private LocalDateTime[] getPlageSemaine(int semaine, int annee) {
        LocalDate lundi = LocalDate.of(annee, 1, 4) // le 4 janvier est toujours en semaine ISO 1
                .with(IsoFields.WEEK_BASED_YEAR, annee)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, semaine)
                .with(DayOfWeek.MONDAY);
        LocalDate dimanche = lundi.plusDays(6);
        return new LocalDateTime[]{ lundi.atStartOfDay(), dimanche.atTime(23, 59, 59) };
    }

    private void validate(ObjectifVendeurRequest r) {
        if (r.getVendeurId() == null) throw new IllegalArgumentException("Le vendeur est obligatoire.");
        if (r.getSemaine() == null || r.getSemaine() < 1 || r.getSemaine() > 53)
            throw new IllegalArgumentException("La semaine doit être entre 1 et 53.");
        if (r.getAnnee() == null || r.getAnnee() < 2000)
            throw new IllegalArgumentException("L'année est invalide.");
        if (r.getObjectifNombreVentes() == null || r.getObjectifNombreVentes() <= 0)
            throw new IllegalArgumentException("L'objectif de nombre de ventes doit être supérieur à 0.");
        if (r.getBonusMontant() == null || r.getBonusMontant() < 0)
            throw new IllegalArgumentException("Le montant du bonus ne peut pas être négatif.");
    }
}
