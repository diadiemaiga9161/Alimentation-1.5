package com.ges.boutique.promo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findAllByOrderByDateDebutDesc();
    List<Promotion> findByActiveTrue();
    List<Promotion> findByActiveTrueAndDateFinGreaterThanEqual(LocalDate today);

    // Promos globales actives aujourd'hui
    @Query("SELECT p FROM Promotion p WHERE p.active = true AND p.globale = true AND p.dateDebut <= :today AND p.dateFin >= :today")
    List<Promotion> findGlobalesActives(@Param("today") LocalDate today);

    // Promos liées à un produit spécifique actives aujourd'hui
    @Query("SELECT p FROM Promotion p JOIN p.produitIds pid WHERE p.active = true AND p.globale = false AND pid = :produitId AND p.dateDebut <= :today AND p.dateFin >= :today")
    List<Promotion> findActivesByProduitId(@Param("produitId") Long produitId, @Param("today") LocalDate today);
}
