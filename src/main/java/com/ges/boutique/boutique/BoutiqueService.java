package com.ges.boutique.boutique;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoutiqueService {

    private final BoutiqueRepository boutiqueRepository;

    public Boutique obtenirBoutique() {
        List<Boutique> boutiques = boutiqueRepository.findAll();
        if (boutiques.isEmpty()) {
            Boutique boutique = new Boutique();
            return boutiqueRepository.save(boutique);
        }
        return boutiques.get(0);
    }

    public Boutique modifierBoutique(Boutique boutique) {
        Boutique existingBoutique = obtenirBoutique();
        existingBoutique.setNom(boutique.getNom());
        existingBoutique.setAdresse(boutique.getAdresse());
        existingBoutique.setTelephone(boutique.getTelephone());
        existingBoutique.setEmail(boutique.getEmail());
        existingBoutique.setNumeroRc(boutique.getNumeroRc());
        existingBoutique.setNumeroIfu(boutique.getNumeroIfu());
        existingBoutique.setVille(boutique.getVille());
        existingBoutique.setPays(boutique.getPays());
        existingBoutique.setCodePostal(boutique.getCodePostal());
        existingBoutique.setSiteWeb(boutique.getSiteWeb());
        existingBoutique.setHorairesOuverture(boutique.getHorairesOuverture());
        existingBoutique.setDescription(boutique.getDescription());
        return boutiqueRepository.save(existingBoutique);
    }

    public Boutique creerBoutique(Boutique boutique) {
        return boutiqueRepository.save(boutique);
    }
}