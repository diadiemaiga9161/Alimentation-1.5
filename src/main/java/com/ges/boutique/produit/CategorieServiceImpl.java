package com.ges.boutique.produit;

import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategorieServiceImpl implements CategorieService {

    private final CategorieRepository categorieRepository;

    @Override
    @Transactional
    public Categorie creerCategorie(Categorie categorie) {
        if (categorie.getNom() == null || categorie.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la catégorie est requis");
        }

        String nomNormalise = categorie.getNom().trim();

        if (categorieRepository.existsByNom(nomNormalise)) {
            throw new RuntimeException("La catégorie '" + nomNormalise + "' existe déjà");
        }

        categorie.setNom(nomNormalise);

        if (categorie.getDescription() != null) {
            categorie.setDescription(categorie.getDescription().trim());
        } else {
            categorie.setDescription("");
        }

        return categorieRepository.save(categorie);
    }

    @Override
    @Transactional
    public Categorie modifierCategorie(Long id, Categorie categorieDetails) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie non trouvée avec l'ID: " + id));

        if (categorieDetails.getNom() != null && !categorieDetails.getNom().trim().isEmpty()) {
            String nouveauNom = categorieDetails.getNom().trim();

            if (!categorie.getNom().equalsIgnoreCase(nouveauNom) &&
                    categorieRepository.existsByNom(nouveauNom)) {
                throw new RuntimeException("La catégorie '" + nouveauNom + "' existe déjà");
            }

            categorie.setNom(nouveauNom);
        }

        if (categorieDetails.getDescription() != null) {
            categorie.setDescription(categorieDetails.getDescription().trim());
        }

        return categorieRepository.save(categorie);
    }

    @Override
    @Transactional
    public void supprimerCategorie(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie non trouvée avec l'ID: " + id));

        categorieRepository.delete(categorie);
    }

    @Override
    public Categorie obtenirCategorieParId(Long id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie non trouvée avec l'ID: " + id));
    }

    @Override
    public List<Categorie> obtenirToutesCategories() {
        return categorieRepository.findAll();
    }

    @Override
    public boolean existeParNom(String nom) {
        return categorieRepository.existsByNom(nom);
    }

    @Override
    public CategorieDto convertirEnDto(Categorie categorie) {
        if (categorie == null) {
            return null;
        }

        CategorieDto dto = new CategorieDto();
        dto.setId(categorie.getId());
        dto.setNom(categorie.getNom());
        dto.setDescription(categorie.getDescription());

        return dto;
    }

    @Override
    public List<CategorieDto> convertirListeEnDto(List<Categorie> categories) {
        return categories.stream()
                .map(this::convertirEnDto)
                .collect(Collectors.toList());
    }
}