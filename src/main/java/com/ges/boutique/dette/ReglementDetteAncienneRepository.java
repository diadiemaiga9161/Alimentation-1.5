package com.ges.boutique.dette;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReglementDetteAncienneRepository extends JpaRepository<ReglementDetteAncienne, Long> {

    @Query("SELECT r FROM ReglementDetteAncienne r WHERE r.dette.id = :detteId ORDER BY r.dateReglement DESC")
    List<ReglementDetteAncienne> findByDetteId(@Param("detteId") Long detteId);

    @Query("SELECT r FROM ReglementDetteAncienne r WHERE r.dateReglement BETWEEN :debut AND :fin ORDER BY r.dateReglement DESC")
    List<ReglementDetteAncienne> findByPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT SUM(r.montantPaye) FROM ReglementDetteAncienne r")
    Double getTotalReglements();

    @Query("SELECT SUM(r.montantPaye) FROM ReglementDetteAncienne r WHERE DATE(r.dateReglement) = CURRENT_DATE")
    Double getTotalReglementsDuJour();

    @Query("SELECT SUM(r.montantPaye) FROM ReglementDetteAncienne r WHERE YEAR(r.dateReglement) = :annee AND MONTH(r.dateReglement) = :mois")
    Double getTotalReglementsParMois(@Param("annee") int annee, @Param("mois") int mois);
}