package com.ges.boutique.caisse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperationCaisseRepository extends JpaRepository<OperationCaisse, Long> {

    List<OperationCaisse> findByDateOperationBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT o FROM OperationCaisse o WHERE DATE(o.dateOperation) = CURRENT_DATE ORDER BY o.dateOperation DESC")
    List<OperationCaisse> findOperationsDuJour();

    @Query("SELECT o FROM OperationCaisse o WHERE o.dateOperation BETWEEN :debut AND :fin ORDER BY o.dateOperation DESC")
    List<OperationCaisse> findOperationsParPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT o FROM OperationCaisse o WHERE o.estReglee = false AND o.type = :type")
    List<OperationCaisse> findCreditsNonRegles(@Param("type") TypeOperationCaisse type);

    @Query("SELECT o FROM OperationCaisse o WHERE o.dateEcheance < :date AND o.estReglee = false")
    List<OperationCaisse> findCreditsEnRetard(@Param("date") LocalDateTime date);

    @Query("SELECT SUM(o.montant) FROM OperationCaisse o WHERE o.type = :type AND o.dateOperation BETWEEN :debut AND :fin")
    Double getTotalByTypeAndPeriod(@Param("type") TypeOperationCaisse type,
                                   @Param("debut") LocalDateTime debut,
                                   @Param("fin") LocalDateTime fin);

    @Query("SELECT o FROM OperationCaisse o WHERE o.venteCreditId = :venteId ORDER BY o.dateOperation DESC")
    List<OperationCaisse> findReglementsByVenteCredit(@Param("venteId") Long venteId);

    @Query("SELECT SUM(o.montant) FROM OperationCaisse o WHERE o.venteCreditId = :venteId AND o.type = :type")
    Double getTotalReglementsByVenteCredit(@Param("venteId") Long venteId,
                                           @Param("type") TypeOperationCaisse type);

    @Query("SELECT COUNT(o) FROM OperationCaisse o WHERE DATE(o.dateOperation) = CURRENT_DATE")
    Long countOperationsDuJour();

    @Query("SELECT o FROM OperationCaisse o WHERE o.vente.id = :venteId AND o.type = :type")
    Optional<OperationCaisse> findFirstByVenteIdAndType(@Param("venteId") Long venteId,
                                                        @Param("type") TypeOperationCaisse type);
}