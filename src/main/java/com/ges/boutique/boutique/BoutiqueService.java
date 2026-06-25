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

        if (boutique.getNom() != null) {
            existingBoutique.setNom(boutique.getNom());
        }

        if (boutique.getAdresse() != null) {
            existingBoutique.setAdresse(boutique.getAdresse());
        }

        if (boutique.getTelephone() != null) {
            existingBoutique.setTelephone(boutique.getTelephone());
        }

        if (boutique.getEmail() != null) {
            existingBoutique.setEmail(boutique.getEmail());
        }

        if (boutique.getNumeroRc() != null) {
            existingBoutique.setNumeroRc(boutique.getNumeroRc());
        }

        if (boutique.getNumeroIfu() != null) {
            existingBoutique.setNumeroIfu(boutique.getNumeroIfu());
        }

        if (boutique.getVille() != null) {
            existingBoutique.setVille(boutique.getVille());
        }

        if (boutique.getPays() != null) {
            existingBoutique.setPays(boutique.getPays());
        }

        if (boutique.getCodePostal() != null) {
            existingBoutique.setCodePostal(boutique.getCodePostal());
        }

        if (boutique.getSiteWeb() != null) {
            existingBoutique.setSiteWeb(boutique.getSiteWeb());
        }

        if (boutique.getHorairesOuverture() != null) {
            existingBoutique.setHorairesOuverture(boutique.getHorairesOuverture());
        }

        if (boutique.getDescription() != null) {
            existingBoutique.setDescription(boutique.getDescription());
        }

        if (boutique.getActif() != null) {
            existingBoutique.setActif(boutique.getActif());
        }

        // Important :
        // Le logo est déjà géré par /api/boutique/upload-logo.
        // Ici on ne supprime jamais le logo existant.
        // Si le front renvoie logo/logoPath, on accepte seulement si la valeur est non vide.

        if (boutique.getLogo() != null && !boutique.getLogo().isBlank()) {
            existingBoutique.setLogo(boutique.getLogo());
        }

        return boutiqueRepository.save(existingBoutique);
    }

    public Boutique saveLogo(String base64Logo) {
        Boutique boutique = obtenirBoutique();
        boutique.setLogo(base64Logo);
        return boutiqueRepository.save(boutique);
    }

    public Boutique creerBoutique(Boutique boutique) {
        return boutiqueRepository.save(boutique);
    }
}