package com.ges.boutique.ia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfilIARepository extends JpaRepository<ProfilIA, Long> {
}
