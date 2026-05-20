package com.ges.boutique.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaiementFournisseurRepository extends JpaRepository<PaiementFournisseur, Long> {
    List<PaiementFournisseur> findByFournisseurIdOrderByDatePaiementDesc(Long fournisseurId);
}