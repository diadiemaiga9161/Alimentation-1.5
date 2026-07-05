package com.ges.boutique.depense;

import java.util.List;

public interface TypeDepenseService {
    List<TypeDepense> getAll();
    TypeDepense creer(String nom);
    TypeDepense modifier(Long id, String nom);
    void supprimer(Long id);
}
