package com.ges.boutique.avance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvanceClientRepository extends JpaRepository<AvanceClient, Long> {

    @Query("SELECT a FROM AvanceClient a WHERE LOWER(a.clientNom) = LOWER(:nom) AND (:telephone IS NULL OR a.clientTelephone = :telephone) ORDER BY a.dateDepot DESC")
    List<AvanceClient> findByClientNomAndTelephone(@Param("nom") String nom, @Param("telephone") String telephone);

    @Query("SELECT COALESCE(SUM(a.montantDisponible), 0) FROM AvanceClient a WHERE LOWER(a.clientNom) = LOWER(:nom) AND a.statut <> 'EPUISE'")
    Double getSoldeDisponibleByNom(@Param("nom") String nom);

    @Query("SELECT a FROM AvanceClient a WHERE a.statut <> 'EPUISE' AND LOWER(a.clientNom) = LOWER(:nom) ORDER BY a.dateDepot ASC")
    List<AvanceClient> findAvancesDisponiblesByNom(@Param("nom") String nom);

    @Query("SELECT a FROM AvanceClient a ORDER BY a.dateDepot DESC")
    List<AvanceClient> findAllOrderByDateDepotDesc();
}
