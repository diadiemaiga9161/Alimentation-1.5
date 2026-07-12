package com.ges.boutique.depot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetraitDepotRepository extends JpaRepository<RetraitDepot, Long> {
    List<RetraitDepot> findByDepotIdOrderByDateRetraitDesc(Long depotId);
}
