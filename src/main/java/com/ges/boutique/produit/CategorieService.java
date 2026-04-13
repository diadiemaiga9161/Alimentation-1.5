package com.ges.boutique.produit;

import java.util.List;

public interface CategorieService {
    Categorie creerCategorie(Categorie categorie);
    Categorie modifierCategorie(Long id, Categorie categorie);
    void supprimerCategorie(Long id);
    Categorie obtenirCategorieParId(Long id);
    List<Categorie> obtenirToutesCategories();
    boolean existeParNom(String nom);
    CategorieDto convertirEnDto(Categorie categorie);
    List<CategorieDto> convertirListeEnDto(List<Categorie> categories);
}