package com.ges.boutique.bonus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BonusFournisseurRepository extends JpaRepository<BonusFournisseur, Long> {

    List<BonusFournisseur> findAllByOrderByDateDesc();

    List<BonusFournisseur> findByFournisseurIdOrderByDateDesc(Long fournisseurId);

    @Query("SELECT b FROM BonusFournisseur b WHERE b.date BETWEEN :debut AND :fin ORDER BY b.date DESC")
    List<BonusFournisseur> findByPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(b.montant), 0) FROM BonusFournisseur b WHERE b.date BETWEEN :debut AND :fin")
    Double sumMontantByPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(b.montant), 0) FROM BonusFournisseur b WHERE YEAR(b.date) = :annee AND MONTH(b.date) = :mois")
    Double sumMontantByMoisAnnee(@Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT COALESCE(SUM(b.montant), 0) FROM BonusFournisseur b WHERE YEAR(b.date) = :annee")
    Double sumMontantByAnnee(@Param("annee") int annee);
}
