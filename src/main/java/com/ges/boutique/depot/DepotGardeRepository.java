package com.ges.boutique.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepotGardeRepository extends JpaRepository<DepotGarde, Long> {

    List<DepotGarde> findAllByOrderByDateDepotDesc();

    List<DepotGarde> findByStatutOrderByDateDepotDesc(StatutDepot statut);

    List<DepotGarde> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrNumeroContaining(
            String nom, String prenom, String numero);

    @Query("SELECT SUM(d.montantRestant) FROM DepotGarde d WHERE d.statut = 'ACTIF'")
    Double getTotalMontantRestantActif();

    @Query("SELECT SUM(d.montantInitial) FROM DepotGarde d")
    Double getTotalMontantInitial();

    List<DepotGarde> findByNumeroAndStatutOrderByDateDepotAsc(String numero, StatutDepot statut);

    List<DepotGarde> findByStatutOrderByDateDepotAsc(StatutDepot statut);
}
