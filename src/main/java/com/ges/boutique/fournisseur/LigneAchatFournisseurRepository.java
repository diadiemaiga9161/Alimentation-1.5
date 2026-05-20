package com.ges.boutique.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneAchatFournisseurRepository extends JpaRepository<LigneAchatFournisseur, Long> {
}