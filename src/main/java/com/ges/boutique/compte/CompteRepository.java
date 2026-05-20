package com.ges.boutique.compte;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CompteRepository extends JpaRepository<Compte, Long> {
    List<Compte> findByActifTrueOrderByNomBanqueAsc();
    List<Compte> findAllByOrderByNomBanqueAsc();
}
