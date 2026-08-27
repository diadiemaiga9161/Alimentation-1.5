package com.ges.boutique.utilisateur;

import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UtilisateurService extends UserDetailsService {

    Utilisateur creerUtilisateur(Utilisateur utilisateur);
    Utilisateur modifierUtilisateur(Long id, Utilisateur utilisateur);
    Utilisateur changerStatutUtilisateur(Long id, boolean actif);
    Utilisateur gererLienEmploye(Long id, LienEmployeRequest request);
    void supprimerUtilisateur(Long id);
    Utilisateur obtenirUtilisateurParId(Long id);
    List<Utilisateur> obtenirTousLesUtilisateurs();
    Utilisateur obtenirUtilisateurParUsername(String username);
    long compterUtilisateurs();
    Utilisateur mettreAJourPhoto(Long id, String photo);
}