package com.ges.boutique.ia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametreModeleIARepository extends JpaRepository<ParametreModeleIA, String> {
}
