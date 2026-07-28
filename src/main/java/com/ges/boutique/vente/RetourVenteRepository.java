package com.ges.boutique.vente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RetourVenteRepository extends JpaRepository<RetourVente, Long> {

    List<RetourVente> findByVenteIdOrderByDateRetourDesc(Long venteId);
    List<RetourVente> findAllByOrderByDateRetourDesc();

    // Variante batch (liste de ventes) — utilisée par le relevé client pour éviter le N+1.
    List<RetourVente> findByVenteIdIn(List<Long> venteIds);

    // ==================== PARAMETRES — SUPPRESSION EN BLOC ====================

    /**
     * Supprime d'abord les lignes_retour_vente (FK vers retours_vente),
     * nécessaire avant de supprimer les retours eux-mêmes.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM lignes_retour_vente WHERE retour_id IN " +
                   "(SELECT id FROM retours_vente WHERE vente_id IN :venteIds)",
           nativeQuery = true)
    int deleteRetourLignesByVenteIds(@Param("venteIds") List<Long> venteIds);

    /**
     * Supprime les retours de vente pour une liste de vente IDs
     * (appeler après deleteRetourLignesByVenteIds).
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM retours_vente WHERE vente_id IN :venteIds",
           nativeQuery = true)
    int deleteRetoursByVenteIds(@Param("venteIds") List<Long> venteIds);
}
