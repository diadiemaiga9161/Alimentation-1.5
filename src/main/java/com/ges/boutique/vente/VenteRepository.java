package com.ges.boutique.vente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VenteRepository extends JpaRepository<Vente, Long> {

    @Query("SELECT v FROM Vente v WHERE v.dateVente BETWEEN :debut AND :fin ORDER BY v.dateVente DESC")
    List<Vente> findByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM Vente v WHERE v.dateVente >= :debut AND v.dateVente <= :fin ORDER BY v.dateVente DESC")
    List<Vente> findTodayVentes(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM Vente v WHERE v.vendeur.id = :vendeurId ORDER BY v.dateVente DESC")
    List<Vente> findByVendeurId(@Param("vendeurId") Long vendeurId);

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateVente DESC")
    List<Vente> findAllCredits();

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND (v.creditRegle IS NULL OR v.creditRegle = false) AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateEcheance ASC")
    List<Vente> findCreditsNonRegles();

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND (v.creditRegle IS NULL OR v.creditRegle = false) AND (v.annulee IS NULL OR v.annulee = false) AND v.dateEcheance < CURRENT_DATE ORDER BY v.dateEcheance ASC")
    List<Vente> findCreditsEnRetard();

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND v.creditRegle = true AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateReglement DESC")
    List<Vente> findCreditsRegles();

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND v.clientNom LIKE %:clientNom% AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateVente DESC")
    List<Vente> findCreditsByClientNom(@Param("clientNom") String clientNom);

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND v.creditRegle = true AND (v.annulee IS NULL OR v.annulee = false) AND v.dateReglement BETWEEN :debut AND :fin ORDER BY v.dateReglement DESC")
    List<Vente> findCreditsReglesByDateRange(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT v FROM Vente v WHERE v.annulee = true ORDER BY v.dateAnnulation DESC")
    List<Vente> findAllVentesAnnulees();

    /**
     * Crédits actifs (non annulés) triés par date de vente décroissante.
     * Endpoint dédié pour éviter le chargement de toutes les ventes côté client.
     */
    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateVente DESC")
    List<Vente> findCreditsActifs();

    @Query("SELECT v FROM Vente v WHERE (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateVente DESC")
    List<Vente> findAllNonAnnulees();

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE v.dateVente >= :debut AND v.dateVente <= :fin AND (v.estCredit IS NULL OR v.estCredit = false) AND (v.annulee IS NULL OR v.annulee = false)")
    Double getChiffreAffaireJournalier(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE v.dateVente >= :debut AND v.dateVente <= :fin AND (v.estCredit IS NULL OR v.estCredit = false) AND (v.annulee IS NULL OR v.annulee = false)")
    Double getChiffreAffaireHebdomadaire(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE v.dateVente >= :debut AND v.dateVente <= :fin AND (v.estCredit IS NULL OR v.estCredit = false) AND (v.annulee IS NULL OR v.annulee = false)")
    Double getChiffreAffaireMensuel(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.montantRestant), 0) FROM Vente v WHERE v.estCredit = true AND (v.creditRegle IS NULL OR v.creditRegle = false) AND (v.annulee IS NULL OR v.annulee = false)")
    Double getTotalCreditsNonRegles();

    @Query("SELECT COALESCE(SUM(v.montantVerse), 0) FROM Vente v WHERE v.estCredit = true AND v.creditRegle = true AND (v.annulee IS NULL OR v.annulee = false) AND v.dateReglement BETWEEN :debutDate AND :finDate")
    Double getTotalReglementsDuJour(@Param("debutDate") LocalDate debutDate, @Param("finDate") LocalDate finDate);

    Optional<Vente> findByClientRequestId(String clientRequestId);

    @Query("SELECT COALESCE(SUM(v.beneficeTotal), 0) FROM Vente v WHERE v.dateVente >= :debut AND v.dateVente <= :fin AND (v.annulee IS NULL OR v.annulee = false)")
    Double getBeneficeTotalByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE v.dateVente >= :debut AND v.dateVente <= :fin AND (v.annulee IS NULL OR v.annulee = false)")
    Double getCAByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.dateVente >= :debut AND v.dateVente <= :fin AND (v.annulee IS NULL OR v.annulee = false)")
    Long countByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM Vente v WHERE v.modePaiement = :mode AND v.dateVente >= :debut AND v.dateVente <= :fin AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateVente DESC")
    List<Vente> findByModePaiementAndDateRange(@Param("mode") ModePaiement mode, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM Vente v WHERE v.modePaiement IN :modes AND v.dateVente >= :debut AND v.dateVente <= :fin AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateVente DESC")
    List<Vente> findByModePaiementInAndDateRange(@Param("modes") List<ModePaiement> modes, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE v.modePaiement = :mode AND v.dateVente >= :debut AND v.dateVente <= :fin AND (v.annulee IS NULL OR v.annulee = false)")
    Double getTotalByModePaiementAndDateRange(@Param("mode") ModePaiement mode, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.modePaiement = :mode AND v.dateVente >= :debut AND v.dateVente <= :fin AND (v.annulee IS NULL OR v.annulee = false)")
    Long countByModePaiementAndDateRange(@Param("mode") ModePaiement mode, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE v.modePaiement IN :modes AND v.dateVente >= :debut AND v.dateVente <= :fin AND (v.annulee IS NULL OR v.annulee = false)")
    Double getTotalMobileMoneyByDateRange(@Param("modes") List<ModePaiement> modes, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE (v.annulee IS NULL OR v.annulee = false) AND v.modePaiement NOT IN :modes")
    Double getTotalEspecesHorsMobileMoney(@Param("modes") List<ModePaiement> modes);

    @Query("SELECT COUNT(v) FROM Vente v WHERE (v.annulee IS NULL OR v.annulee = false) AND v.dateVente >= :debut AND v.dateVente <= :fin AND v.estCredit = false")
    Long countVentesComptantByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v) FROM Vente v WHERE (v.annulee IS NULL OR v.annulee = false) AND v.dateVente >= :debut AND v.dateVente <= :fin AND v.estCredit = true")
    Long countVentesCreditByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM Vente v LEFT JOIN FETCH v.lignes WHERE v.client.id = :clientId AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateVente DESC")
    List<Vente> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT v FROM Vente v LEFT JOIN FETCH v.lignes WHERE (v.clientNom = :nom OR v.clientTelephone = :telephone) AND (v.annulee IS NULL OR v.annulee = false) ORDER BY v.dateVente DESC")
    List<Vente> findByClientNomOrTelephone(@Param("nom") String nom, @Param("telephone") String telephone);
}