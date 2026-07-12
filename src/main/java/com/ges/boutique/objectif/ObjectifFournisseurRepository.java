package com.ges.boutique.objectif;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ObjectifFournisseurRepository extends JpaRepository<ObjectifFournisseur, Long> {

    List<ObjectifFournisseur> findByMoisAndAnneeOrderByDateCreationDesc(int mois, int annee);

    List<ObjectifFournisseur> findByFournisseurIdOrderByAnneeDescMoisDesc(Long fournisseurId);

    List<ObjectifFournisseur> findByAnneeOrderByMoisDescDateCreationDesc(int annee);

    List<ObjectifFournisseur> findAllByOrderByAnneeDescMoisDescDateCreationDesc();

    @Query("SELECT COALESCE(SUM(o.bonusCalcule), 0) FROM ObjectifFournisseur o WHERE o.mois = :mois AND o.annee = :annee AND o.statut = 'ATTEINT'")
    Double getTotalBonusParMoisAnnee(@Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT COALESCE(SUM(o.bonusCalcule), 0) FROM ObjectifFournisseur o WHERE o.annee = :annee AND o.statut = 'ATTEINT'")
    Double getTotalBonusParAnnee(@Param("annee") int annee);

    @Query("SELECT COUNT(o) FROM ObjectifFournisseur o WHERE o.mois = :mois AND o.annee = :annee AND o.statut = 'ATTEINT'")
    long countAtteintsByMoisAnnee(@Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT COUNT(o) FROM ObjectifFournisseur o WHERE o.mois = :mois AND o.annee = :annee")
    long countByMoisAnnee(@Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT COALESCE(SUM(o.quantiteBonusRecue), 0) FROM ObjectifFournisseur o WHERE o.mois = :mois AND o.annee = :annee")
    Double getTotalQuantiteBonusRecueParMoisAnnee(@Param("mois") int mois, @Param("annee") int annee);

    List<ObjectifFournisseur> findByFournisseurIdAndMoisAndAnneeAndStockAjouteFalse(Long fournisseurId, int mois, int annee);
}
