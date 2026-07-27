package com.ges.boutique.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaiementFournisseurRepository extends JpaRepository<PaiementFournisseur, Long> {
    List<PaiementFournisseur> findByFournisseurIdOrderByDatePaiementDesc(Long fournisseurId);

    @Query("SELECT p FROM PaiementFournisseur p WHERE p.datePaiement BETWEEN :dateDebut AND :dateFin ORDER BY p.datePaiement DESC")
    List<PaiementFournisseur> findByPeriode(@Param("dateDebut") LocalDateTime dateDebut,
                                            @Param("dateFin") LocalDateTime dateFin);

    List<PaiementFournisseur> findAllByOrderByDatePaiementDesc();

    List<PaiementFournisseur> findByFournisseurIdAndDatePaiementBetweenOrderByDatePaiementDesc(
            Long fournisseurId, LocalDateTime debut, LocalDateTime fin);
}