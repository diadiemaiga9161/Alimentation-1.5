package com.ges.boutique.caisse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
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

    // Filtre sur le montantRestant de la vente liée (source de vérité) plutôt que sur le
    // flag dénormalisé o.estReglee : ce flag n'est mis à jour que lors d'un règlement de
    // crédit, jamais lors d'un retour ou d'une modification de vente qui repasse le montant
    // restant au-dessus de 0 — il peut donc rester bloqué à "réglé" alors que la vente ne
    // l'est plus (vu en prod sur 2 boutiques, session du 2026-08-02). Repli sur o.estReglee
    // uniquement quand il n'y a pas de vente liée (ex: crédit divers sans vente).
    @Query("SELECT o FROM OperationCaisse o WHERE o.type = :type " +
           "AND (o.vente IS NULL OR o.vente.annulee = false OR o.vente.annulee IS NULL) " +
           "AND ((o.vente IS NOT NULL AND (o.vente.montantRestant IS NULL OR o.vente.montantRestant > 0.01)) " +
           "OR (o.vente IS NULL AND o.estReglee = false))")
    List<OperationCaisse> findCreditsNonRegles(@Param("type") TypeOperationCaisse type);

    // MODIFIÉ: Exclure les crédits dont la vente est annulée
    @Query("SELECT o FROM OperationCaisse o WHERE o.dateEcheance < :date " +
           "AND (o.vente IS NULL OR o.vente.annulee = false OR o.vente.annulee IS NULL) " +
           "AND ((o.vente IS NOT NULL AND (o.vente.montantRestant IS NULL OR o.vente.montantRestant > 0.01)) " +
           "OR (o.vente IS NULL AND o.estReglee = false))")
    List<OperationCaisse> findCreditsEnRetard(@Param("date") LocalDateTime date);

    @Query("SELECT SUM(o.montant) FROM OperationCaisse o WHERE o.type = :type AND o.dateOperation BETWEEN :debut AND :fin")
    Double getTotalByTypeAndPeriod(@Param("type") TypeOperationCaisse type,
                                   @Param("debut") LocalDateTime debut,
                                   @Param("fin") LocalDateTime fin);

    @Query("SELECT o FROM OperationCaisse o WHERE o.venteCreditId = :venteId ORDER BY o.dateOperation DESC")
    List<OperationCaisse> findReglementsByVenteCredit(@Param("venteId") Long venteId);

    @Query("SELECT COUNT(o) FROM OperationCaisse o WHERE o.caisse.id = :caisseId AND DATE(o.dateOperation) = CURRENT_DATE")
    Long countOperationsDuJourByCaisseId(@Param("caisseId") Long caisseId);

    @Query("SELECT SUM(o.montant) FROM OperationCaisse o WHERE o.venteCreditId = :venteId AND o.type = :type")
    Double getTotalReglementsByVenteCredit(@Param("venteId") Long venteId,
                                           @Param("type") TypeOperationCaisse type);

    @Query("SELECT COUNT(o) FROM OperationCaisse o WHERE DATE(o.dateOperation) = CURRENT_DATE")
    Long countOperationsDuJour();

    @Query("SELECT o FROM OperationCaisse o WHERE o.vente.id = :venteId AND o.type = :type")
    Optional<OperationCaisse> findFirstByVenteIdAndType(@Param("venteId") Long venteId,
                                                        @Param("type") TypeOperationCaisse type);

    // Nouvelle méthode pour trouver l'opération de vente comptant d'une vente
    @Query("SELECT o FROM OperationCaisse o WHERE o.vente.id = :venteId AND o.type = :type")
    Optional<OperationCaisse> findOperationVenteByVenteIdAndType(@Param("venteId") Long venteId,
                                                                 @Param("type") TypeOperationCaisse type);

    // Tous les règlements de crédit sur une période
    @Query("SELECT o FROM OperationCaisse o WHERE o.type = 'REGLEMENT_CREDIT' AND o.dateOperation BETWEEN :dateDebut AND :dateFin ORDER BY o.dateOperation DESC")
    List<OperationCaisse> findReglementsByPeriode(@Param("dateDebut") LocalDateTime dateDebut,
                                                  @Param("dateFin") LocalDateTime dateFin);

    // Tous les règlements de crédit sans filtre de période
    @Query("SELECT o FROM OperationCaisse o WHERE o.type = 'REGLEMENT_CREDIT' ORDER BY o.dateOperation DESC")
    List<OperationCaisse> findAllReglements();

    // Trouver une opération par venteCreditId et type (ex : VENTE_CREDIT pour une vente donnée)
    Optional<OperationCaisse> findByVenteCreditIdAndType(Long venteCreditId, TypeOperationCaisse type);

    /**
     * Tous les règlements (versements) actifs pour une liste de ventes à crédit — utilisé par le
     * relevé client (situation client) pour construire l'historique chronologique en une seule
     * requête batch (évite le pattern N+1 de l'ancien flux forkJoin côté front).
     * Filtre explicitement le type REGLEMENT_CREDIT : une même venteCreditId est aussi portée par
     * l'opération VENTE_CREDIT initiale (montant = total du crédit), qu'il ne faut surtout pas
     * confondre avec un versement sous peine de fausser le calcul du reliquat.
     */
    @Query("SELECT o FROM OperationCaisse o WHERE o.venteCreditId IN :venteCreditIds " +
           "AND o.type = 'REGLEMENT_CREDIT' AND o.annule = false ORDER BY o.dateOperation ASC")
    List<OperationCaisse> findReglementsByVenteCreditIdIn(@Param("venteCreditIds") List<Long> venteCreditIds);

    // ==================== PARAMETRES — NETTOYAGE ====================

    /**
     * Annule la référence vente pour les opérations caisse liées aux ventes
     * qui vont être supprimées (vente_id est nullable dans operations_caisse).
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE OperationCaisse o SET o.vente = null WHERE o.vente.id IN :venteIds")
    int nullOutVenteReferences(@Param("venteIds") List<Long> venteIds);

    /**
     * Supprime les opérations de caisse antérieures à la date donnée,
     * en conservant les crédits VENTE_CREDIT non encore réglés (actifs).
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM OperationCaisse o WHERE o.dateOperation < :date " +
           "AND NOT (o.type = 'VENTE_CREDIT' AND o.estReglee = false)")
    int deleteOldOperationsExceptActiveCredits(@Param("date") LocalDateTime date);

    // ========== PAGE PARAMÈTRES : RÉINITIALISATION / SUPPRESSION HISTORIQUE ==========

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM OperationCaisse o WHERE o.dateOperation BETWEEN :debut AND :fin")
    void deleteByDateOperationBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM OperationCaisse o")
    void deleteAllOperations();
}