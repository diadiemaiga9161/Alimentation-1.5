package com.ges.boutique.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByNumeroTelephone(String numeroTelephone);

    @Query("SELECT c FROM Client c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%')) OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))")
    List<Client> findByNomContainingOrPrenomContaining(String nom, String prenom);

    @Query("SELECT c FROM Client c ORDER BY c.dateCreation DESC")
    List<Client> findAllOrderByDateCreationDesc();

    @Query("SELECT c, COUNT(v) as nombreAchats, COALESCE(SUM(v.montantTotal), 0) as montantTotal " +
            "FROM Client c LEFT JOIN c.ventes v " +
            "WHERE v.annulee IS NULL OR v.annulee = false " +
            "GROUP BY c " +
            "ORDER BY montantTotal DESC")
    List<Object[]> findTopClientsByMontant();

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.client.id = :clientId AND (v.annulee IS NULL OR v.annulee = false)")
    long countVentesActivesByClientId(@Param("clientId") Long clientId);

    boolean existsByNumeroTelephone(String numeroTelephone);
}