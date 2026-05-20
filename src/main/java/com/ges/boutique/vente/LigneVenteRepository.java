package com.ges.boutique.vente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LigneVenteRepository extends JpaRepository<LigneVente, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM LigneVente l WHERE l.vente.id = :venteId")
    void deleteAllByVenteId(@Param("venteId") Long venteId);
}