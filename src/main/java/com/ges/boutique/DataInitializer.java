package com.ges.boutique;

import com.ges.boutique.boutique.Boutique;
import com.ges.boutique.boutique.BoutiqueRepository;
import com.ges.boutique.utilisateur.RoleUtilisateur;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final BoutiqueRepository boutiqueRepository;

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("=========================================");
        System.out.println("Démarrage de l'initialisation des données...");
        System.out.println("=========================================");

        // Créer les utilisateurs par défaut si aucun n'existe
        if (utilisateurRepository.count() == 0) {
            creerUtilisateursParDefaut();
        }

        // Créer la boutique par défaut si aucune n'existe
        if (boutiqueRepository.count() == 0) {
            creerBoutiqueParDefaut();
        }

        System.out.println("=========================================");
        System.out.println("Initialisation terminée avec succès!");
        System.out.println("=========================================");
    }

    private void creerUtilisateursParDefaut() {
        // Admin
        Utilisateur admin = new Utilisateur();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNomComplet("Administrateur Principal");
        admin.setEmail("admin@boutique.com");
        admin.setTelephone("+2230102030405");
        admin.setRole(RoleUtilisateur.ADMIN);
        admin.setActif(true);
        utilisateurRepository.save(admin);

        // Vendeur
        Utilisateur vendeur = new Utilisateur();
        vendeur.setUsername("vendeur");
        vendeur.setPassword(passwordEncoder.encode("vendeur123"));
        vendeur.setNomComplet("Vendeur Principal");
        vendeur.setEmail("vendeur@boutique.com");
        vendeur.setTelephone("+2230506070809");
        vendeur.setRole(RoleUtilisateur.VENDEUR);
        vendeur.setActif(true);
        utilisateurRepository.save(vendeur);

        // Gestionnaire de stock
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setUsername("gestionnaire");
        gestionnaire.setPassword(passwordEncoder.encode("gestion123"));
        gestionnaire.setNomComplet("Gestionnaire de Stock");
        gestionnaire.setEmail("gestionnaire@boutique.com");
        gestionnaire.setTelephone("+2230910111213");
        gestionnaire.setRole(RoleUtilisateur.ADMIN);
        gestionnaire.setActif(true);
        utilisateurRepository.save(gestionnaire);

        System.out.println("✅ Utilisateurs par défaut créés.");
    }

    private void creerBoutiqueParDefaut() {
        Boutique boutique = new Boutique();
        boutique.setNom("Boutique de Test");
        boutique.setDescription("Boutique créée pour les tests");
        boutique.setAdresse("123 Rue de la Paix, Bamako");
        boutique.setTelephone("+223 20 30 40 50");
        boutique.setEmail("contact@boutiquedetest.ml");
        boutique.setSiteWeb("www.boutiquedetest.ml");
        boutique.setHorairesOuverture("Lu-Ve: 8h-18h, Sa: 9h-17h");
        boutique.setActif(true);
        boutiqueRepository.save(boutique);

        System.out.println("✅ Boutique par défaut créée: Boutique de Test");
    }
}