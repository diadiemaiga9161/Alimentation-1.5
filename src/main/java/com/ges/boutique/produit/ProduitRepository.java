package com.ges.boutique.produit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByCategorieId(Long categorieId);

    List<Produit> findByFournisseurId(Long fournisseurId);

    List<Produit> findByNomContainingIgnoreCase(String nom);

    List<Produit> findByCodeBarre(String codeBarre);

    @Query("SELECT p FROM Produit p WHERE p.quantite <= p.seuilAlerte")
    List<Produit> trouverProduitsStockFaible();

    @Query("SELECT p FROM Produit p WHERE p.quantite = 0")
    List<Produit> trouverProduitsEnRupture();

    @Query("SELECT COUNT(p) FROM Produit p WHERE p.quantite <= p.seuilAlerte")
    Long compterProduitsStockFaible();

    @Query("SELECT SUM(p.prixAchat * p.quantite) FROM Produit p")
    Double getValeurTotaleStock();

    boolean existsByNomAndCategorieId(String nom, Long categorieId);

    List<Produit> findByDatePeremptionBefore(LocalDate date);

    List<Produit> findByDatePeremptionBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT p FROM Produit p WHERE p.datePeremption <= :dateAlerte AND p.datePeremption >= CURRENT_DATE")
    List<Produit> trouverProduitsProchePeremption(@Param("dateAlerte") LocalDate dateAlerte);

    @Query("SELECT p FROM Produit p WHERE p.bio = true")
    List<Produit> trouverProduitsBio();

    List<Produit> findByOrigine(String origine);

    @Query("SELECT p FROM Produit p WHERE p.dateCreation >= :dateDebut")
    List<Produit> trouverProduitsRecents(@Param("dateDebut") LocalDate dateDebut);

    @Query("SELECT COUNT(l) FROM LigneVente l WHERE l.produit.id = :produitId")
    long countLignesVenteByProduitId(@Param("produitId") Long produitId);

    long countByFournisseurId(Long fournisseurId);

    List<Produit> findByNomIgnoreCase(String nom);

}
