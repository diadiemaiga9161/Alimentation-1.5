package com.ges.boutique.caisse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaisseRepository extends JpaRepository<Caisse, Long> {

    @Query("SELECT c FROM Caisse c WHERE c.estOuverte = true")
    Optional<Caisse> findCaisseOuverte();

    Optional<Caisse> findFirstByOrderByIdDesc();

    @Query("SELECT c FROM Caisse c WHERE c.dateOuverture BETWEEN :debut AND :fin ORDER BY c.dateOuverture DESC")
    List<Caisse> findByPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT SUM(c.soldeActuel) FROM Caisse c WHERE c.estOuverte = true")
    Double getSoldeTotalCaisse();

    @Query("SELECT COUNT(c) FROM Caisse c WHERE c.estOuverte = true")
    Long countCaisseOuverte();

    @Query("SELECT c FROM Caisse c WHERE c.verifiee = false AND c.estOuverte = false")
    List<Caisse> findCaissesNonVerifiees();

    boolean existsByNumeroCaisse(String numeroCaisse);

    Optional<Caisse> findByNumeroCaisse(String numeroCaisse);
}