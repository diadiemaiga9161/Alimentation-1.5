package com.ges.boutique.transfert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoutiquePartenaireRepository extends JpaRepository<BoutiquePartenaire, Long> {
    List<BoutiquePartenaire> findByActifTrue();
}
