package com.ges.boutique.facture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    List<Facture> findByStatut(String statut);

    List<Facture> findByClientId(Long clientId);

    Facture findByNumeroFacture(String numeroFacture);

    List<Facture> findByStatutAndClientId(String statut, Long clientId);

    @Query("SELECT f FROM Facture f WHERE f.statut NOT IN ('PAYEE', 'ANNULEE')")
    List<Facture> findFacturesNonPayees();

    @Query("SELECT f FROM Facture f WHERE f.client.id = :clientId AND f.statut NOT IN ('PAYEE', 'ANNULEE')")
    List<Facture> findFacturesNonPayeesByClient(@Param("clientId") Long clientId);

    List<Facture> findByUtilisateurId(Long utilisateurId);

    List<Facture> findByVenteId(Long venteId);

    List<Facture> findByDateCreationBetween(LocalDateTime dateDebut, LocalDateTime dateFin);

    List<Facture> findByClientNomContainingIgnoreCase(String clientNom);
}