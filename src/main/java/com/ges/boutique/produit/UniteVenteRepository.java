package com.ges.boutique.produit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniteVenteRepository extends JpaRepository<UniteVente, Long> {
    List<UniteVente> findByProduitIdOrderByOrdreAsc(Long produitId);
}
