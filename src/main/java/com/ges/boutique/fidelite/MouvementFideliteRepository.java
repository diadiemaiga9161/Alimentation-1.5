package com.ges.boutique.fidelite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MouvementFideliteRepository extends JpaRepository<MouvementFidelite, Long> {
    List<MouvementFidelite> findByClientIdOrderByDateMouvementDesc(Long clientId);
    List<MouvementFidelite> findByVenteIdAndAnnuleFalse(Long venteId);
}
