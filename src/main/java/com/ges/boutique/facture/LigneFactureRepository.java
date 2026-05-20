package com.ges.boutique.facture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LigneFactureRepository extends JpaRepository<LigneFacture, Long> {

    List<LigneFacture> findByFactureId(Long factureId);

    @Transactional
    void deleteByFactureId(Long factureId);
}