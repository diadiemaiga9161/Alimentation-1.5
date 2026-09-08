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

        // Compte super admin (Amadou Maiga) : vérifié à CHAQUE démarrage (pas seulement
        // sur une base vide), pour que le compte apparaisse automatiquement aussi bien
        // sur une nouvelle boutique que sur une boutique existante après mise à jour du
        // backend. Ne fait que CRÉER si absent — ne touche jamais un compte existant,
        // pour ne pas écraser un mot de passe déjà changé par l'intéressé.
        assurerCompteSuperAdmin();

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

    private void assurerCompteSuperAdmin() {
        final String username = "amadou";
        final String nomComplet = "Amadou Maiga";
        final String email = "91613489.diadie@gmail.com";

        // Certaines boutiques ont déjà un compte "amadou" créé avant ce système (compte
        // personnel utilisé de longue date, parfois avec un nom/email/mot de passe
        // différents selon la boutique) : on le PROMEUT sur place et on aligne nom, email
        // et mot de passe pour que ce soit exactement le même compte partout. Ce
        // réalignement du mot de passe ne se fait qu'UNE SEULE FOIS, au moment précis de
        // la promotion (première fois que superAdmin passe à true) — une fois promu, les
        // démarrages suivants ne retouchent plus jamais le mot de passe, pour ne pas
        // écraser un changement fait entre-temps par l'intéressé lui-même.
        var existant = utilisateurRepository.findByUsername(username);
        if (existant.isPresent()) {
            Utilisateur u = existant.get();
            boolean modifie = false;
            boolean premierePromotion = !u.isSuperAdmin();
            if (premierePromotion) {
                u.setSuperAdmin(true);
                u.setPassword(passwordEncoder.encode("Diadie2026"));
                modifie = true;
            }
            // Le privilège super admin exige le rôle ADMIN (voir @PreAuthorize sur
            // /api/boutique/fonctionnalites) — sans ça la promotion serait inopérante.
            if (u.getRole() != RoleUtilisateur.ADMIN) {
                u.setRole(RoleUtilisateur.ADMIN);
                modifie = true;
            }
            if (!nomComplet.equals(u.getNomComplet())) {
                u.setNomComplet(nomComplet);
                modifie = true;
            }
            if (!email.equalsIgnoreCase(u.getEmail())) {
                // L'email est unique en base : on ne l'aligne que si aucun AUTRE compte ne
                // l'utilise déjà dans cette boutique (sinon on laisserait planter la sauvegarde).
                boolean prisParUnAutre = utilisateurRepository.findByEmail(email)
                        .filter(autre -> !autre.getId().equals(u.getId()))
                        .isPresent();
                if (prisParUnAutre) {
                    System.out.println("⚠️ Email du compte super admin non aligné : " + email + " déjà utilisé par un autre compte dans cette boutique.");
                } else {
                    u.setEmail(email);
                    modifie = true;
                }
            }
            if (modifie) {
                utilisateurRepository.save(u);
                System.out.println("✅ Compte existant promu/aligné super admin : " + username);
            }
            return;
        }

        if (utilisateurRepository.findByEmail(email).isPresent()) {
            // Un autre compte utilise déjà cet email sous un autre identifiant — on ne
            // force rien pour éviter un conflit, l'admin devra régulariser lui-même.
            System.out.println("⚠️ Compte super admin non créé : l'email " + email + " est déjà utilisé par un autre compte.");
            return;
        }

        Utilisateur superAdmin = new Utilisateur();
        superAdmin.setUsername(username);
        superAdmin.setPassword(passwordEncoder.encode("Diadie2026"));
        superAdmin.setNomComplet("Amadou Maiga");
        superAdmin.setEmail(email);
        superAdmin.setTelephone("+22300000000");
        superAdmin.setRole(RoleUtilisateur.ADMIN);
        superAdmin.setActif(true);
        superAdmin.setSuperAdmin(true);
        utilisateurRepository.save(superAdmin);

        System.out.println("✅ Compte super admin créé : " + username);
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