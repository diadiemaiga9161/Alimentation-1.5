package com.ges.boutique.journalaudit;

import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface JournalAuditService {

    /**
     * Enregistre une entrée dans le journal d'audit. Méthode interne, appelée par les
     * autres services métier — il n'existe volontairement aucun endpoint d'écriture public.
     * Robuste par construction : ne doit jamais faire échouer l'action métier appelante
     * (toute erreur technique est loguée et avalée, jamais propagée).
     */
    void enregistrer(Long utilisateurId, String utilisateurNom, TypeActionAudit action, String details);

    /**
     * Consultation paginée avec filtres optionnels (dateDebut/dateFin/utilisateurId).
     * Un filtre non fourni (null) est simplement ignoré. dateDebut/dateFin sont des dates
     * simples (pas d'heure) — comme les autres filtres de période de l'app — étendues en
     * interne sur la journée complète (00:00:00 à 23:59:59) pour filtrer dateAction.
     */
    Page<JournalAuditDto> rechercher(LocalDate dateDebut, LocalDate dateFin, Long utilisateurId,
                                      int page, int size);
}
