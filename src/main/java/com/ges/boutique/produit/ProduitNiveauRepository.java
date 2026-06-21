package com.ges.boutique.produit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ProduitNiveauRepository extends JpaRepository<ProduitNiveau, Long> {
    List<ProduitNiveau> findByProduitIdOrderByOrdreAsc(Long produitId);

    @Transactional
    void deleteByProduitId(Long produitId);
}
