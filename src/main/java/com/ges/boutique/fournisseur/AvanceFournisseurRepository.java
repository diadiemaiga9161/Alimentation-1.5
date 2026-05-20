package com.ges.boutique.fournisseur;

import com.ges.boutique.avance.StatutAvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AvanceFournisseurRepository extends JpaRepository<AvanceFournisseur, Long> {

    @Query("SELECT a FROM AvanceFournisseur a WHERE a.fournisseur.id = :fournisseurId ORDER BY a.dateDepot DESC")
    List<AvanceFournisseur> findByFournisseurIdOrderByDateDesc(@Param("fournisseurId") Long fournisseurId);

    @Query("SELECT COALESCE(SUM(a.montantDisponible), 0) FROM AvanceFournisseur a WHERE a.fournisseur.id = :fournisseurId AND a.statut <> 'EPUISE'")
    Double getSoldeDisponibleByFournisseurId(@Param("fournisseurId") Long fournisseurId);

    @Query("SELECT a FROM AvanceFournisseur a WHERE a.fournisseur.id = :fournisseurId AND a.statut <> 'EPUISE' ORDER BY a.dateDepot ASC")
    List<AvanceFournisseur> findAvancesDisponiblesByFournisseurId(@Param("fournisseurId") Long fournisseurId);

    @Query("SELECT a FROM AvanceFournisseur a ORDER BY a.dateDepot DESC")
    List<AvanceFournisseur> findAllOrderByDateDesc();
}
