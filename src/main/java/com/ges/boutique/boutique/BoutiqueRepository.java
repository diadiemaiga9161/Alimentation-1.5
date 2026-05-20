package com.ges.boutique.boutique;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {
}