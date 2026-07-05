package com.ges.boutique.depense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DepenseRepository extends JpaRepository<Depense, Long> {
    List<Depense> findAllByOrderByDateDesc();
    List<Depense> findByDateBetweenOrderByDateDesc(LocalDate debut, LocalDate fin);
    List<Depense> findByDateOrderByCreatedAtDesc(LocalDate date);

    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.date BETWEEN :debut AND :fin")
    Double sumMontantByPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d WHERE YEAR(d.date) = :annee AND MONTH(d.date) = :mois")
    Double sumMontantByMoisAnnee(@Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d WHERE YEAR(d.date) = :annee")
    Double sumMontantByAnnee(@Param("annee") int annee);

    @Query("SELECT d.typeDepense, SUM(d.montant) FROM Depense d WHERE d.typeDepense IS NOT NULL GROUP BY d.typeDepense ORDER BY SUM(d.montant) DESC")
    List<Object[]> getTotauxParType();

    @Query("SELECT d.typeDepense, SUM(d.montant) FROM Depense d WHERE d.typeDepense IS NOT NULL AND d.date BETWEEN :debut AND :fin GROUP BY d.typeDepense ORDER BY SUM(d.montant) DESC")
    List<Object[]> getTotauxParTypePeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    boolean existsByTypeDepense(String typeDepense);
}
