package com.ges.boutique.vente;

import com.ges.boutique.produit.Produit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires purs (aucune base de donnees, aucun contexte Spring) sur
 * le calcul du sous-total d'une ligne de vente. Logique financiere critique
 * signalee dans l'analyse projet (section 7, point 9 : "aucun test
 * unitaire n'est execute") -- calculerSousTotal() est protected, testable
 * directement car ce test est dans le meme package com.ges.boutique.vente.
 */
class LigneVenteTest {

    private Produit produit(double prixVente, double prixAchat) {
        Produit p = new Produit();
        p.setPrixVente(prixVente);
        p.setPrixAchat(prixAchat);
        return p;
    }

    @Test
    void sousTotal_sansRemise_estPrixFoisQuantite() {
        LigneVente ligne = new LigneVente();
        ligne.setProduit(produit(1000.0, 600.0));
        ligne.setPrixUnitaire(1000.0);
        ligne.setQuantite(3);

        ligne.calculerSousTotal();

        assertEquals(3000.0, ligne.getSousTotal());
        assertEquals(1200.0, ligne.getBenefice()); // (1000-600)*3
    }

    @Test
    void sousTotal_avecRemisePourcentage_reduitLePrixUnitaire() {
        LigneVente ligne = new LigneVente();
        ligne.setProduit(produit(1000.0, 600.0));
        ligne.setPrixUnitaire(1000.0);
        ligne.setQuantite(2);
        ligne.setRemisePourcentage(10.0);

        ligne.calculerSousTotal();

        assertEquals(900.0, ligne.getPrixApresRemise());
        assertEquals(1800.0, ligne.getSousTotal());
    }

    @Test
    void sousTotal_avecRemiseMontant_reduitLePrixUnitaire() {
        LigneVente ligne = new LigneVente();
        ligne.setProduit(produit(1000.0, 600.0));
        ligne.setPrixUnitaire(1000.0);
        ligne.setQuantite(1);
        ligne.setRemiseMontant(200.0);

        ligne.calculerSousTotal();

        assertEquals(800.0, ligne.getPrixApresRemise());
        assertEquals(800.0, ligne.getSousTotal());
    }

    @Test
    void sousTotal_remiseSuperieureAuPrix_neDevientJamaisNegatif() {
        LigneVente ligne = new LigneVente();
        ligne.setProduit(produit(1000.0, 600.0));
        ligne.setPrixUnitaire(1000.0);
        ligne.setQuantite(1);
        ligne.setRemiseMontant(5000.0); // remise absurde superieure au prix

        ligne.calculerSousTotal();

        assertEquals(0.0, ligne.getPrixApresRemise());
        assertEquals(0.0, ligne.getSousTotal());
    }

    @Test
    void getMontantRemise_reneteLecartAvantEtApresRemise() {
        LigneVente ligne = new LigneVente();
        ligne.setProduit(produit(1000.0, 600.0));
        ligne.setPrixUnitaire(1000.0);
        ligne.setQuantite(2);
        ligne.setRemisePourcentage(10.0);

        ligne.calculerSousTotal();

        // sans remise: 2000, avec remise: 1800 -> remise totale = 200
        assertEquals(200.0, ligne.getMontantRemise());
    }

    @Test
    void appliquerRemisePourcentage_ecraseUneRemiseMontantPrecedente() {
        LigneVente ligne = new LigneVente();
        ligne.setProduit(produit(1000.0, 600.0));
        ligne.setPrixUnitaire(1000.0);
        ligne.setQuantite(1);
        ligne.setRemiseMontant(300.0);
        ligne.calculerSousTotal();
        assertEquals(700.0, ligne.getSousTotal());

        ligne.appliquerRemisePourcentage(10.0);

        assertEquals(0.0, ligne.getRemiseMontant());
        assertEquals(900.0, ligne.getSousTotal());
    }
}
