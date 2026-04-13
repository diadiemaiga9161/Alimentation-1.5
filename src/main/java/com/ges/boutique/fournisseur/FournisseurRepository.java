package com.ges.boutique.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    Optional<Fournisseur> findByCode(String code);

    Optional<Fournisseur> findByNom(String nom);

    List<Fournisseur> findByNomContainingIgnoreCase(String nom);

    List<Fournisseur> findByActifTrue();

    @Query("SELECT f FROM Fournisseur f WHERE f.typeProduits LIKE %:type%")
    List<Fournisseur> findByTypeProduitsContaining(String type);

    boolean existsByCode(String code);

    boolean existsByNom(String nom);

    @Query("SELECT f FROM Fournisseur f ORDER BY f.note DESC")
    List<Fournisseur> trouverMeilleursFournisseurs();
}