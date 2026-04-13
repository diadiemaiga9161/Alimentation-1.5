package com.ges.boutique.inventaire;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByProduitId(Long produitId);
    List<MouvementStock> findByDateMouvementBetween(LocalDateTime debut, LocalDateTime fin);
    List<MouvementStock> findByTypeMouvement(TypeMouvement typeMouvement);
}