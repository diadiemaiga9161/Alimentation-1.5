package com.ges.boutique.journalaudit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {

    Page<JournalAudit> findByDateActionBetween(LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable);

    Page<JournalAudit> findByUtilisateurId(Long utilisateurId, Pageable pageable);

    Page<JournalAudit> findByUtilisateurIdAndDateActionBetween(
            Long utilisateurId, LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable);

    // findAll(Pageable) est déjà fourni par JpaRepository (cas sans aucun filtre)
}
