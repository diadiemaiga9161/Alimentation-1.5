package com.ges.boutique.journalaudit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalAuditServiceImpl implements JournalAuditService {

    private final JournalAuditRepository journalAuditRepository;

    @Override
    @Transactional
    public void enregistrer(Long utilisateurId, String utilisateurNom, TypeActionAudit action, String details) {
        // L'échec de cet enregistrement ne doit JAMAIS faire échouer l'action métier
        // appelante : toute exception technique est loguée puis avalée, jamais relancée.
        try {
            JournalAudit entree = new JournalAudit();
            entree.setUtilisateurId(utilisateurId);
            entree.setUtilisateurNom(utilisateurNom);
            entree.setAction(action);
            entree.setDetails(details);
            entree.setDateAction(LocalDateTime.now());
            journalAuditRepository.save(entree);
        } catch (Exception e) {
            log.error("Échec de l'enregistrement du journal d'audit (action={}, utilisateurId={}, details={}) : {}",
                    action, utilisateurId, details, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalAuditDto> rechercher(LocalDate dateDebut, LocalDate dateFin, Long utilisateurId,
                                             int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 20,
                Sort.by(Sort.Direction.DESC, "dateAction"));

        // dateDebut/dateFin sont des dates simples (ex: 2026-08-24), envoyées telles quelles
        // par les 3 apps (même convention que les autres filtres de période de l'app) — on les
        // étend ici sur la journée complète pour filtrer le champ dateAction (LocalDateTime).
        boolean filtrePeriode = dateDebut != null || dateFin != null;
        LocalDateTime debut = null;
        LocalDateTime fin = null;
        if (filtrePeriode) {
            debut = dateDebut != null ? dateDebut.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
            fin = dateFin != null ? LocalDateTime.of(dateFin, LocalTime.of(23, 59, 59)) : LocalDateTime.now();
        }

        Page<JournalAudit> resultat;
        if (utilisateurId != null && filtrePeriode) {
            resultat = journalAuditRepository.findByUtilisateurIdAndDateActionBetween(utilisateurId, debut, fin, pageable);
        } else if (utilisateurId != null) {
            resultat = journalAuditRepository.findByUtilisateurId(utilisateurId, pageable);
        } else if (filtrePeriode) {
            resultat = journalAuditRepository.findByDateActionBetween(debut, fin, pageable);
        } else {
            resultat = journalAuditRepository.findAll(pageable);
        }

        return resultat.map(this::convertToDto);
    }

    private JournalAuditDto convertToDto(JournalAudit entite) {
        JournalAuditDto dto = new JournalAuditDto();
        dto.setId(entite.getId());
        dto.setUtilisateurId(entite.getUtilisateurId());
        dto.setUtilisateurNom(entite.getUtilisateurNom());
        dto.setAction(entite.getAction());
        dto.setDetails(entite.getDetails());
        dto.setDateAction(entite.getDateAction());
        return dto;
    }
}
