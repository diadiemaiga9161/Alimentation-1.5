package com.ges.boutique.inventaire;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByProduitId(Long produitId);
    List<MouvementStock> findByDateMouvementBetween(LocalDateTime debut, LocalDateTime fin);
    List<MouvementStock> findByTypeMouvement(TypeMouvement typeMouvement);
    List<MouvementStock> findByAchatId(Long achatId);
    List<MouvementStock> findAllByOrderByDateMouvementDesc();

    @Query("SELECT m FROM MouvementStock m WHERE m.typeMouvement = com.ges.boutique.inventaire.TypeMouvement.SORTIE " +
           "AND (:typeSortie IS NULL OR m.typeSortie = :typeSortie) " +
           "AND (:utilisateurId IS NULL OR m.utilisateur.id = :utilisateurId) " +
           "AND (:produitId IS NULL OR m.produit.id = :produitId) " +
           "AND (:dateDebut IS NULL OR m.dateMouvement >= :dateDebut) " +
           "AND (:dateFin IS NULL OR m.dateMouvement <= :dateFin) " +
           "ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findSorties(@Param("typeSortie") String typeSortie,
                                      @Param("utilisateurId") Long utilisateurId,
                                      @Param("produitId") Long produitId,
                                      @Param("dateDebut") LocalDateTime dateDebut,
                                      @Param("dateFin") LocalDateTime dateFin);
}