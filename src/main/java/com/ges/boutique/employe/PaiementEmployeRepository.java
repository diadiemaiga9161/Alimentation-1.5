package com.ges.boutique.employe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaiementEmployeRepository extends JpaRepository<PaiementEmploye, Long> {

    List<PaiementEmploye> findByEmployeIdOrderByDatePaiementDesc(Long employeId);

    List<PaiementEmploye> findAllByOrderByDatePaiementDesc();

    List<PaiementEmploye> findByStatutOrderByDatePaiementDesc(StatutPaiementEmploye statut);

    @Query("SELECT p FROM PaiementEmploye p WHERE p.datePaiement BETWEEN :debut AND :fin ORDER BY p.datePaiement DESC")
    List<PaiementEmploye> findByPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM PaiementEmploye p WHERE p.statut = 'PAYE'")
    Double getTotalPaiementsEffectues();

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM PaiementEmploye p WHERE p.statut = 'PAYE' AND p.datePaiement BETWEEN :debut AND :fin")
    Double sumMontantByPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM PaiementEmploye p WHERE p.statut = 'PAYE' AND YEAR(p.datePaiement) = :annee AND MONTH(p.datePaiement) = :mois")
    Double sumMontantByMoisAnnee(@Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM PaiementEmploye p WHERE p.statut = 'PAYE' AND YEAR(p.datePaiement) = :annee")
    Double sumMontantByAnnee(@Param("annee") int annee);
}
