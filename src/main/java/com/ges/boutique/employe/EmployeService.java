package com.ges.boutique.employe;

import java.util.List;

public interface EmployeService {
    EmployeDto creerEmploye(EmployeRequest request);
    EmployeDto modifierEmploye(Long id, EmployeRequest request);
    EmployeDto getEmployeById(Long id);
    List<EmployeDto> getTousLesEmployes();
    List<EmployeDto> getEmployesActifs();
    void desactiverEmploye(Long id);
    void activerEmploye(Long id);
    void supprimerEmploye(Long id);
}
