package com.ges.boutique.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FonctionnaliteBoutiqueRepository extends JpaRepository<FonctionnaliteBoutique, Long> {
    Optional<FonctionnaliteBoutique> findByCle(CleFonctionnalite cle);
}
