package com.ges.boutique;

import com.ges.boutique.fournisseur.Fournisseur;
import com.ges.boutique.fournisseur.FournisseurRepository;
import com.ges.boutique.produit.*;
import com.ges.boutique.utilisateur.RoleUtilisateur;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;
    private final FournisseurRepository fournisseurRepository;

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("=========================================");
        System.out.println("Démarrage de l'initialisation des données...");
        System.out.println("=========================================");

        // Créer l'admin par défaut si n'existe pas
        if (utilisateurRepository.count() == 0) {
            creerUtilisateursParDefaut();
        }

        // Créer des fournisseurs par défaut
        if (fournisseurRepository.count() == 0) {
            creerFournisseursParDefaut();
        }

        // Créer des catégories par défaut pour les produits alimentaires
        if (categorieRepository.count() == 0) {
            creerCategoriesAlimentairesParDefaut();
        }

        // Créer des produits de démonstration alimentaires
        if (produitRepository.count() == 0) {
            creerProduitsAlimentairesDemonstration();
        }

        System.out.println("=========================================");
        System.out.println("Initialisation terminée avec succès!");
        System.out.println("Application prête à l'utilisation!");
        System.out.println("API disponible sur: http://localhost:8080/api");
        System.out.println("Swagger UI: http://localhost:8080/api/swagger-ui.html");
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

        System.out.println("✅ Utilisateurs par défaut créés:");
        System.out.println("   Admin: admin / admin123");
        System.out.println("   Vendeur: vendeur / vendeur123");
        System.out.println("   Gestionnaire: gestionnaire / gestion123");
    }

    private void creerFournisseursParDefaut() {
        // Fournisseur de produits laitiers
        Fournisseur laiterieModerne = new Fournisseur();
        laiterieModerne.setNom("Laiterie Moderne");
        laiterieModerne.setCode("LAIT001");
        laiterieModerne.setAdresse("Zone Industrielle, Bamako");
        laiterieModerne.setTelephone("+223 20 22 33 44");
        laiterieModerne.setEmail("contact@laiteriemoderne.ml");
        laiterieModerne.setContactNom("Mamadou Diallo");
        laiterieModerne.setContactTelephone("+223 70 11 22 33");
        laiterieModerne.setContactEmail("m.diallo@laiteriemoderne.ml");
        laiterieModerne.setDescription("Fournisseur de produits laitiers frais");
        laiterieModerne.setTypeProduits("Produits laitiers, Yaourts, Fromages");
        laiterieModerne.setConditionsPaiement("30 jours");
        laiterieModerne.setDelaiLivraison(2);
        laiterieModerne.setNote(5);
        laiterieModerne.setActif(true);
        fournisseurRepository.save(laiterieModerne);

        // Fournisseur de fruits et légumes
        Fournisseur primeursMali = new Fournisseur();
        primeursMali.setNom("Primeurs du Mali");
        primeursMali.setCode("PRIM002");
        primeursMali.setAdresse("Marché de Lafiabougou, Bamako");
        primeursMali.setTelephone("+223 66 55 44 33");
        primeursMali.setEmail("info@primeursdumali.ml");
        primeursMali.setContactNom("Fatoumata Traoré");
        primeursMali.setContactTelephone("+223 75 55 66 77");
        primeursMali.setContactEmail("f.traore@primeursdumali.ml");
        primeursMali.setDescription("Fruits et légumes frais locaux et importés");
        primeursMali.setTypeProduits("Fruits, Légumes, Herbes aromatiques");
        primeursMali.setConditionsPaiement("Comptant");
        primeursMali.setDelaiLivraison(1);
        primeursMali.setNote(4);
        primeursMali.setActif(true);
        fournisseurRepository.save(primeursMali);

        // Fournisseur de produits secs et épicerie
        Fournisseur epicerieTradition = new Fournisseur();
        epicerieTradition.setNom("Épicerie Tradition");
        epicerieTradition.setCode("EPIC003");
        epicerieTradition.setAdresse("Badalabougou, Bamako");
        epicerieTradition.setTelephone("+223 20 24 24 24");
        epicerieTradition.setEmail("contact@epicerietradition.ml");
        epicerieTradition.setSiteWeb("www.epicerietradition.ml");
        epicerieTradition.setContactNom("Ousmane Keita");
        epicerieTradition.setContactTelephone("+223 76 33 44 55");
        epicerieTradition.setDescription("Produits d'épicerie, conserves, boissons");
        epicerieTradition.setTypeProduits("Épicerie, Boissons, Conserves");
        epicerieTradition.setConditionsPaiement("15 jours");
        epicerieTradition.setDelaiLivraison(3);
        epicerieTradition.setNote(4);
        epicerieTradition.setActif(true);
        fournisseurRepository.save(epicerieTradition);

        // Fournisseur de produits bio
        Fournisseur bioSaveurs = new Fournisseur();
        bioSaveurs.setNom("Bio Saveurs");
        bioSaveurs.setCode("BIO004");
        bioSaveurs.setAdresse("Koulouba, Bamako");
        bioSaveurs.setTelephone("+223 74 44 55 66");
        bioSaveurs.setEmail("contact@biosaveurs.ml");
        bioSaveurs.setContactNom("Aminata Diarra");
        bioSaveurs.setDescription("Produits biologiques certifiés");
        bioSaveurs.setTypeProduits("Produits bio, Céréales, Légumineuses");
        bioSaveurs.setConditionsPaiement("30 jours");
        bioSaveurs.setDelaiLivraison(4);
        bioSaveurs.setNote(5);
        bioSaveurs.setActif(true);
        fournisseurRepository.save(bioSaveurs);

        System.out.println("✅ Fournisseurs par défaut créés: 4 fournisseurs");
    }

    private void creerCategoriesAlimentairesParDefaut() {
        // Catégories principales
        Categorie produitsLaitiers = new Categorie();
        produitsLaitiers.setNom("Produits Laitiers");
        produitsLaitiers.setDescription("Lait, yaourts, fromages, beurre, crème");
        categorieRepository.save(produitsLaitiers);

        Categorie fruitsLegumes = new Categorie();
        fruitsLegumes.setNom("Fruits et Légumes");
        fruitsLegumes.setDescription("Frais, de saison, locaux et importés");
        categorieRepository.save(fruitsLegumes);

        Categorie epicerieSalee = new Categorie();
        epicerieSalee.setNom("Épicerie Salée");
        epicerieSalee.setDescription("Pâtes, riz, conserves, sauces");
        categorieRepository.save(epicerieSalee);

        Categorie epicerieSucree = new Categorie();
        epicerieSucree.setNom("Épicerie Sucrée");
        epicerieSucree.setDescription("Biscuits, confitures, chocolats, desserts");
        categorieRepository.save(epicerieSucree);

        Categorie boissons = new Categorie();
        boissons.setNom("Boissons");
        boissons.setDescription("Jus, sodas, eaux, sirops");
        categorieRepository.save(boissons);

        Categorie viandesPoissons = new Categorie();
        viandesPoissons.setNom("Viandes et Poissons");
        viandesPoissons.setDescription("Frais, surgelés, charcuterie");
        categorieRepository.save(viandesPoissons);

        Categorie surgeles = new Categorie();
        surgeles.setNom("Surgelés");
        surgeles.setDescription("Légumes surgelés, plats préparés, glaces");
        categorieRepository.save(surgeles);

        Categorie bio = new Categorie();
        bio.setNom("Bio");
        bio.setDescription("Produits biologiques certifiés");
        categorieRepository.save(bio);

        System.out.println("✅ Catégories alimentaires créées: 8 catégories");
    }

    private void creerProduitsAlimentairesDemonstration() {
        // Récupérer les fournisseurs
        Fournisseur laiterieModerne = fournisseurRepository.findByCode("LAIT001").orElseThrow();
        Fournisseur primeursMali = fournisseurRepository.findByCode("PRIM002").orElseThrow();
        Fournisseur epicerieTradition = fournisseurRepository.findByCode("EPIC003").orElseThrow();
        Fournisseur bioSaveurs = fournisseurRepository.findByCode("BIO004").orElseThrow();

        // Récupérer les catégories
        Categorie produitsLaitiers = categorieRepository.findByNom("Produits Laitiers").orElseThrow();
        Categorie fruitsLegumes = categorieRepository.findByNom("Fruits et Légumes").orElseThrow();
        Categorie epicerieSalee = categorieRepository.findByNom("Épicerie Salée").orElseThrow();
        Categorie epicerieSucree = categorieRepository.findByNom("Épicerie Sucrée").orElseThrow();
        Categorie boissons = categorieRepository.findByNom("Boissons").orElseThrow();
        Categorie bio = categorieRepository.findByNom("Bio").orElseThrow();

        // Produits laitiers
        Produit lait = new Produit();
        lait.setNom("Lait Demi-écrémé");
        lait.setDescription("Lait frais pasteurisé 1L");
        lait.setCategorie(produitsLaitiers);
        lait.setFournisseur(laiterieModerne);
        lait.setPrixAchat(600.0);
        lait.setPrixVente(950.0);
        lait.setQuantite(120);
        lait.setSeuilAlerte(20);
        lait.setCodeBarre("3760012345001");
        lait.setDateCreation(LocalDate.now().minusDays(5));
        lait.setDatePeremption(LocalDate.now().plusDays(12));
        lait.setLotNumber("LAIT-02-2024");
        lait.setConditionsStockage("Frais, entre 2°C et 6°C");
        lait.setPoidsVolume(1.0);
        lait.setUniteMesure("L");
        lait.setBio(false);
        lait.setOrigine("Mali");
        produitRepository.save(lait);

        Produit yaourt = new Produit();
        yaourt.setNom("Yaourt Nature");
        yaourt.setDescription("Yaourt nature 125g x 4");
        yaourt.setCategorie(produitsLaitiers);
        yaourt.setFournisseur(laiterieModerne);
        yaourt.setPrixAchat(450.0);
        yaourt.setPrixVente(750.0);
        yaourt.setQuantite(80);
        yaourt.setSeuilAlerte(15);
        yaourt.setCodeBarre("3760012345002");
        yaourt.setDateCreation(LocalDate.now().minusDays(3));
        yaourt.setDatePeremption(LocalDate.now().plusDays(18));
        yaourt.setLotNumber("YAOURT-02-2024");
        yaourt.setConditionsStockage("Frais, entre 2°C et 6°C");
        yaourt.setPoidsVolume(500.0);
        yaourt.setUniteMesure("g");
        yaourt.setBio(false);
        yaourt.setOrigine("Mali");
        produitRepository.save(yaourt);

        // Fruits et légumes
        Produit bananes = new Produit();
        bananes.setNom("Bananes");
        bananes.setDescription("Bananes mûres, origine locale");
        bananes.setCategorie(fruitsLegumes);
        bananes.setFournisseur(primeursMali);
        bananes.setPrixAchat(400.0);
        bananes.setPrixVente(650.0);
        bananes.setQuantite(45);
        bananes.setSeuilAlerte(10);
        bananes.setCodeBarre("3760012345003");
        bananes.setDateCreation(LocalDate.now().minusDays(2));
        bananes.setDatePeremption(LocalDate.now().plusDays(5));
        bananes.setLotNumber("BAN-15-03");
        bananes.setConditionsStockage("Température ambiante");
        bananes.setPoidsVolume(1.0);
        bananes.setUniteMesure("kg");
        bananes.setBio(false);
        bananes.setOrigine("Mali");
        produitRepository.save(bananes);

        Produit pommes = new Produit();
        pommes.setNom("Pommes Golden");
        pommes.setDescription("Pommes Golden, France");
        pommes.setCategorie(fruitsLegumes);
        pommes.setFournisseur(primeursMali);
        pommes.setPrixAchat(1500.0);
        pommes.setPrixVente(2200.0);
        pommes.setQuantite(30);
        pommes.setSeuilAlerte(8);
        pommes.setCodeBarre("3760012345004");
        pommes.setDateCreation(LocalDate.now().minusDays(7));
        pommes.setDatePeremption(LocalDate.now().plusDays(10));
        pommes.setLotNumber("POM-10-03");
        pommes.setConditionsStockage("Température ambiante");
        pommes.setPoidsVolume(1.0);
        pommes.setUniteMesure("kg");
        pommes.setBio(false);
        pommes.setOrigine("France");
        produitRepository.save(pommes);

        // Épicerie salée
        Produit riz = new Produit();
        riz.setNom("Riz Parfumé");
        riz.setDescription("Riz long grain 5kg");
        riz.setCategorie(epicerieSalee);
        riz.setFournisseur(epicerieTradition);
        riz.setPrixAchat(2500.0);
        riz.setPrixVente(3750.0);
        riz.setQuantite(25);
        riz.setSeuilAlerte(5);
        riz.setCodeBarre("3760012345005");
        riz.setDateCreation(LocalDate.now().minusDays(30));
        riz.setDatePeremption(LocalDate.now().plusMonths(11));
        riz.setLotNumber("RIZ-001-2024");
        riz.setConditionsStockage("Sec, à l'abri de l'humidité");
        riz.setPoidsVolume(5.0);
        riz.setUniteMesure("kg");
        riz.setBio(false);
        riz.setOrigine("Thaïlande");
        produitRepository.save(riz);

        Produit pates = new Produit();
        pates.setNom("Pâtes Spaghetti");
        pates.setDescription("Pâtes spaghetti n°5 500g");
        pates.setCategorie(epicerieSalee);
        pates.setFournisseur(epicerieTradition);
        pates.setPrixAchat(350.0);
        pates.setPrixVente(600.0);
        pates.setQuantite(60);
        pates.setSeuilAlerte(15);
        pates.setCodeBarre("3760012345006");
        pates.setDateCreation(LocalDate.now().minusDays(20));
        pates.setDatePeremption(LocalDate.now().plusMonths(23));
        pates.setLotNumber("PAST-02-2024");
        pates.setConditionsStockage("Sec, à l'abri de l'humidité");
        pates.setPoidsVolume(500.0);
        pates.setUniteMesure("g");
        pates.setBio(false);
        pates.setOrigine("Italie");
        produitRepository.save(pates);

        // Boissons
        Produit jusOrange = new Produit();
        jusOrange.setNom("Jus d'Orange");
        jusOrange.setDescription("Pur jus d'orange 1L");
        jusOrange.setCategorie(boissons);
        jusOrange.setFournisseur(epicerieTradition);
        jusOrange.setPrixAchat(750.0);
        jusOrange.setPrixVente(1200.0);
        jusOrange.setQuantite(35);
        jusOrange.setSeuilAlerte(8);
        jusOrange.setCodeBarre("3760012345007");
        jusOrange.setDateCreation(LocalDate.now().minusDays(15));
        jusOrange.setDatePeremption(LocalDate.now().plusMonths(4));
        jusOrange.setLotNumber("JUS-05-2024");
        jusOrange.setConditionsStockage("Après ouverture, conserver au frais");
        jusOrange.setPoidsVolume(1.0);
        jusOrange.setUniteMesure("L");
        jusOrange.setBio(false);
        jusOrange.setOrigine("Espagne");
        produitRepository.save(jusOrange);

        // Produits bio
        Produit farineBio = new Produit();
        farineBio.setNom("Farine de Blé Bio");
        farineBio.setDescription("Farine de blé T65 bio 1kg");
        farineBio.setCategorie(bio);
        farineBio.setFournisseur(bioSaveurs);
        farineBio.setPrixAchat(800.0);
        farineBio.setPrixVente(1350.0);
        farineBio.setQuantite(20);
        farineBio.setSeuilAlerte(5);
        farineBio.setCodeBarre("3760012345008");
        farineBio.setDateCreation(LocalDate.now().minusDays(10));
        farineBio.setDatePeremption(LocalDate.now().plusMonths(8));
        farineBio.setLotNumber("FAR-BIO-001");
        farineBio.setConditionsStockage("Sec, à l'abri de la lumière");
        farineBio.setPoidsVolume(1.0);
        farineBio.setUniteMesure("kg");
        farineBio.setBio(true);
        farineBio.setOrigine("France");
        produitRepository.save(farineBio);

        Produit lentillesBio = new Produit();
        lentillesBio.setNom("Lentilles Vertes Bio");
        lentillesBio.setDescription("Lentilles vertes bio 500g");
        lentillesBio.setCategorie(bio);
        lentillesBio.setFournisseur(bioSaveurs);
        lentillesBio.setPrixAchat(650.0);
        lentillesBio.setPrixVente(1100.0);
        lentillesBio.setQuantite(18);
        lentillesBio.setSeuilAlerte(4);
        lentillesBio.setCodeBarre("3760012345009");
        lentillesBio.setDateCreation(LocalDate.now().minusDays(12));
        lentillesBio.setDatePeremption(LocalDate.now().plusMonths(10));
        lentillesBio.setLotNumber("LENT-BIO-002");
        lentillesBio.setConditionsStockage("Sec, à l'abri de l'humidité");
        lentillesBio.setPoidsVolume(500.0);
        lentillesBio.setUniteMesure("g");
        lentillesBio.setBio(true);
        lentillesBio.setOrigine("France");
        produitRepository.save(lentillesBio);

        // Produit avec stock faible pour tester les alertes
        Produit fromage = new Produit();
        fromage.setNom("Fromage Blanc");
        fromage.setDescription("Fromage blanc 250g");
        fromage.setCategorie(produitsLaitiers);
        fromage.setFournisseur(laiterieModerne);
        fromage.setPrixAchat(550.0);
        fromage.setPrixVente(950.0);
        fromage.setQuantite(3); // Stock faible
        fromage.setSeuilAlerte(5);
        fromage.setCodeBarre("3760012345010");
        fromage.setDateCreation(LocalDate.now().minusDays(4));
        fromage.setDatePeremption(LocalDate.now().plusDays(6));
        fromage.setLotNumber("FROM-03-2024");
        fromage.setConditionsStockage("Frais, entre 2°C et 6°C");
        fromage.setPoidsVolume(250.0);
        fromage.setUniteMesure("g");
        fromage.setBio(false);
        fromage.setOrigine("Mali");
        produitRepository.save(fromage);

        // Produit proche de péremption pour tester les alertes
        Produit creme = new Produit();
        creme.setNom("Crème Fraîche");
        creme.setDescription("Crème fraîche épaisse 200ml");
        creme.setCategorie(produitsLaitiers);
        creme.setFournisseur(laiterieModerne);
        creme.setPrixAchat(450.0);
        creme.setPrixVente(800.0);
        creme.setQuantite(8);
        creme.setSeuilAlerte(3);
        creme.setCodeBarre("3760012345011");
        creme.setDateCreation(LocalDate.now().minusDays(10));
        creme.setDatePeremption(LocalDate.now().plusDays(2)); // Proche péremption
        creme.setLotNumber("CREME-02-2024");
        creme.setConditionsStockage("Frais, entre 2°C et 6°C");
        creme.setPoidsVolume(200.0);
        creme.setUniteMesure("ml");
        creme.setBio(false);
        creme.setOrigine("Mali");
        produitRepository.save(creme);

        System.out.println("✅ Produits alimentaires de démonstration créés: 11 produits");
    }
}