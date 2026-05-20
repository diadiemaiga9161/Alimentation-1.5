package com.ges.boutique.dette;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetteAncienneRepository extends JpaRepository<DetteAncienne, Long> {

    @Query("SELECT d FROM DetteAncienne d WHERE d.client.id = :clientId ORDER BY d.dateCredit DESC")
    List<DetteAncienne> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT d FROM DetteAncienne d WHERE d.estReglee = false ORDER BY d.dateCredit ASC")
    List<DetteAncienne> findDettesNonReglees();

    @Query("SELECT d FROM DetteAncienne d WHERE d.estReglee = true ORDER BY d.dateDernierReglement DESC")
    List<DetteAncienne> findDettesReglees();

    @Query("SELECT d FROM DetteAncienne d ORDER BY d.dateCredit DESC")
    List<DetteAncienne> findAllOrderByDateCreditDesc();

    @Query("SELECT d FROM DetteAncienne d WHERE LOWER(d.client.nom) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(d.client.prenom) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR d.client.numeroTelephone LIKE CONCAT('%', :search, '%')")
    List<DetteAncienne> searchByClient(@Param("search") String search);

    @Query("SELECT SUM(d.montantRestant) FROM DetteAncienne d WHERE d.estReglee = false")
    Double getTotalDettesRestantes();

    @Query("SELECT COUNT(d) FROM DetteAncienne d WHERE d.estReglee = false")
    Long countDettesNonReglees();

    @Query("SELECT SUM(d.montantInitial) FROM DetteAncienne d")
    Double getTotalDettesInitiales();
}