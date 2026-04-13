package com.ges.boutique.vente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VenteRepository extends JpaRepository<Vente, Long> {

    @Query("SELECT v FROM Vente v WHERE v.dateVente BETWEEN :debut AND :fin ORDER BY v.dateVente DESC")
    List<Vente> findByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM Vente v WHERE DATE(v.dateVente) = CURRENT_DATE ORDER BY v.dateVente DESC")
    List<Vente> findTodayVentes();

    @Query("SELECT v FROM Vente v WHERE v.vendeur.id = :vendeurId ORDER BY v.dateVente DESC")
    List<Vente> findByVendeurId(@Param("vendeurId") Long vendeurId);

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.dateVente BETWEEN :debut AND :fin")
    Long countVentesByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE DATE(v.dateVente) = CURRENT_DATE AND (v.estCredit IS NULL OR v.estCredit = false) AND v.annulee = false")
    Double getChiffreAffaireJournalier();

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE YEAR(v.dateVente) = YEAR(CURRENT_DATE) AND WEEK(v.dateVente) = WEEK(CURRENT_DATE) AND (v.estCredit IS NULL OR v.estCredit = false) AND v.annulee = false")
    Double getChiffreAffaireHebdomadaire();

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE MONTH(v.dateVente) = MONTH(CURRENT_DATE) AND YEAR(v.dateVente) = YEAR(CURRENT_DATE) AND (v.estCredit IS NULL OR v.estCredit = false) AND v.annulee = false")
    Double getChiffreAffaireMensuel();

    @Query("SELECT v.modePaiement, COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE (v.estCredit IS NULL OR v.estCredit = false) AND v.annulee = false GROUP BY v.modePaiement")
    List<Object[]> getChiffreAffaireParModePaiement();

    // Requêtes pour les crédits (excluant les annulées)
    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND v.annulee = false ORDER BY v.dateVente DESC")
    List<Vente> findAllCredits();

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND v.creditRegle = false AND v.annulee = false ORDER BY v.dateEcheance ASC")
    List<Vente> findCreditsNonRegles();

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND v.creditRegle = false AND v.annulee = false AND v.dateEcheance < CURRENT_DATE ORDER BY v.dateEcheance ASC")
    List<Vente> findCreditsEnRetard();

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND v.clientNom LIKE %:clientNom% AND v.annulee = false ORDER BY v.dateVente DESC")
    List<Vente> findCreditsByClientNom(@Param("clientNom") String clientNom);

    @Query("SELECT v FROM Vente v WHERE v.estCredit = true AND v.creditRegle = true AND v.annulee = false AND v.dateReglement BETWEEN :debut AND :fin ORDER BY v.dateReglement DESC")
    List<Vente> findCreditsReglesByDateRange(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(v.montantRestant), 0) FROM Vente v WHERE v.estCredit = true AND v.creditRegle = false AND v.annulee = false")
    Double getTotalCreditsNonRegles();

    @Query("SELECT COALESCE(SUM(v.montantVerse), 0) FROM Vente v WHERE v.estCredit = true AND v.creditRegle = true AND v.annulee = false AND v.dateReglement = CURRENT_DATE")
    Double getTotalReglementsDuJour();

    @Query("SELECT COALESCE(SUM(v.montantVerse), 0) FROM Vente v WHERE v.estCredit = true AND v.creditRegle = true AND v.annulee = false AND v.dateReglement BETWEEN :debut AND :fin")
    Double getTotalReglementsParPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    // Requêtes pour les ventes annulées
    @Query("SELECT v FROM Vente v WHERE v.annulee = true ORDER BY v.dateAnnulation DESC")
    List<Vente> findAllVentesAnnulees();

    @Query("SELECT v FROM Vente v WHERE v.annulee = false ORDER BY v.dateVente DESC")
    List<Vente> findAllNonAnnulees();
}