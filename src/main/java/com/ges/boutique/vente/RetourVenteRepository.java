package com.ges.boutique.vente;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RetourVenteRepository extends JpaRepository<RetourVente, Long> {
    List<RetourVente> findByVenteIdOrderByDateRetourDesc(Long venteId);
    List<RetourVente> findAllByOrderByDateRetourDesc();
}
