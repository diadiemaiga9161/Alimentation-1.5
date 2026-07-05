package com.ges.boutique.depense;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class TypeDepenseServiceImpl implements TypeDepenseService {

    private static final List<String> TYPES_PAR_DEFAUT = Arrays.asList(
            "Loyer",
            "Électricité",
            "Eau",
            "Internet",
            "Transport",
            "Salaire",
            "Achat stock",
            "Maintenance",
            "Publicité",
            "Taxes",
            "Fournitures bureau",
            "Restauration",
            "Autre"
    );

    private final TypeDepenseRepository typeDepenseRepository;
    private final DepenseRepository depenseRepository;

    public TypeDepenseServiceImpl(TypeDepenseRepository typeDepenseRepository,
                                  DepenseRepository depenseRepository) {
        this.typeDepenseRepository = typeDepenseRepository;
        this.depenseRepository = depenseRepository;
    }

    @Override
    @Transactional
    public List<TypeDepense> getAll() {
        List<TypeDepense> liste = typeDepenseRepository.findAll();
        if (liste.isEmpty()) {
            for (String nom : TYPES_PAR_DEFAUT) {
                if (!typeDepenseRepository.existsByNom(nom)) {
                    typeDepenseRepository.save(new TypeDepense(nom));
                }
            }
            liste = typeDepenseRepository.findAll();
        }
        return liste;
    }

    @Override
    @Transactional
    public TypeDepense creer(String nom) {
        String nomTrimmed = nom.trim();
        if (typeDepenseRepository.existsByNom(nomTrimmed)) {
            throw new RuntimeException("Un type de dépense avec ce nom existe déjà : " + nomTrimmed);
        }
        return typeDepenseRepository.save(new TypeDepense(nomTrimmed));
    }

    @Override
    @Transactional
    public TypeDepense modifier(Long id, String nom) {
        TypeDepense type = typeDepenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de dépense introuvable, id=" + id));
        String nomTrimmed = nom.trim();
        if (!type.getNom().equals(nomTrimmed) && typeDepenseRepository.existsByNom(nomTrimmed)) {
            throw new RuntimeException("Un type de dépense avec ce nom existe déjà : " + nomTrimmed);
        }
        type.setNom(nomTrimmed);
        return typeDepenseRepository.save(type);
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        TypeDepense type = typeDepenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de dépense introuvable, id=" + id));
        if (depenseRepository.existsByTypeDepense(type.getNom())) {
            throw new RuntimeException("Type utilisé par des dépenses existantes");
        }
        typeDepenseRepository.deleteById(id);
    }
}
