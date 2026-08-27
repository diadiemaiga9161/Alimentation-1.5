package com.ges.boutique.objectifvendeur;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjectifVendeurRepository extends JpaRepository<ObjectifVendeur, Long> {

    List<ObjectifVendeur> findBySemaineAndAnneeOrderByDateCreationDesc(int semaine, int annee);

    List<ObjectifVendeur> findByVendeurIdOrderByAnneeDescSemaineDesc(Long vendeurId);

    List<ObjectifVendeur> findByAnneeOrderBySemaineDescDateCreationDesc(int annee);

    List<ObjectifVendeur> findAllByOrderByAnneeDescSemaineDescDateCreationDesc();
}
