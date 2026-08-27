package com.ges.boutique.vente;

import com.ges.boutique.produit.Produit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires purs sur le calcul des totaux d'une vente (Vente.calculerTotal)
 * et le reglement des credits (Vente.enregistrerReglement). Logique financiere
 * critique -- aucune base de donnees ni contexte Spring necessaire.
 */
class VenteTest {

    private LigneVente ligne(double prixUnitaire, int quantite) {
        LigneVente l = new LigneVente();
        Produit p = new Produit();
        p.setPrixVente(prixUnitaire);
        p.setPrixAchat(prixUnitaire * 0.6);
        l.setProduit(p);
        l.setPrixUnitaire(prixUnitaire);
        l.setQuantite(quantite);
        l.calculerSousTotal();
        return l;
    }

    @Test
    void calculerTotal_sommeLesSousTotauxDesLignes() {
        Vente vente = new Vente();
        vente.ajouterLigne(ligne(1000.0, 2)); // 2000
        vente.ajouterLigne(ligne(500.0, 3));  // 1500

        assertEquals(3500.0, vente.getMontantTotal());
    }

    @Test
    void calculerTotal_avecRemiseGlobalePourcentage() {
        Vente vente = new Vente();
        vente.ajouterLigne(ligne(1000.0, 1)); // 1000

        vente.appliquerRemiseGlobalePourcentage(10.0);

        assertEquals(900.0, vente.getMontantTotal());
        assertEquals(100.0, vente.getMontantRemiseTotal());
    }

    @Test
    void calculerTotal_remiseGlobaleMontant_estPlafonneeAuSousTotal() {
        Vente vente = new Vente();
        vente.ajouterLigne(ligne(1000.0, 1)); // 1000

        vente.appliquerRemiseGlobaleMontant(5000.0); // remise absurde

        assertEquals(0.0, vente.getMontantTotal());
    }

    @Test
    void enregistrerReglement_reduitLeMontantRestantEtMarqueSoldeQuandComplet() {
        Vente vente = new Vente();
        vente.ajouterLigne(ligne(1000.0, 1)); // 1000
        vente.setEstCredit(true);
        vente.calculerTotal(); // montantRestant = 1000 - 0 - 0

        vente.enregistrerReglement(400.0, java.time.LocalDate.now());
        assertEquals(600.0, vente.getMontantRestant());
        assertFalse(vente.getCreditRegle());

        vente.enregistrerReglement(600.0, java.time.LocalDate.now());
        assertEquals(0.0, vente.getMontantRestant());
        assertTrue(vente.getCreditRegle());
    }

    @Test
    void enregistrerReglement_refuseUnMontantSuperieurAuRestant() {
        Vente vente = new Vente();
        vente.ajouterLigne(ligne(1000.0, 1));
        vente.setEstCredit(true);
        vente.calculerTotal();

        assertThrows(IllegalArgumentException.class,
                () -> vente.enregistrerReglement(2000.0, java.time.LocalDate.now()));
    }

    @Test
    void enregistrerReglement_refuseUnMontantNulOuNegatif() {
        Vente vente = new Vente();
        vente.ajouterLigne(ligne(1000.0, 1));
        vente.setEstCredit(true);
        vente.calculerTotal();

        assertThrows(IllegalArgumentException.class,
                () -> vente.enregistrerReglement(0.0, java.time.LocalDate.now()));
        assertThrows(IllegalArgumentException.class,
                () -> vente.enregistrerReglement(-50.0, java.time.LocalDate.now()));
    }

    @Test
    void supprimerLigne_recalculeLeTotal() {
        Vente vente = new Vente();
        LigneVente l1 = ligne(1000.0, 1);
        LigneVente l2 = ligne(500.0, 1);
        vente.ajouterLigne(l1);
        vente.ajouterLigne(l2);
        assertEquals(1500.0, vente.getMontantTotal());

        vente.supprimerLigne(l2);

        assertEquals(1000.0, vente.getMontantTotal());
        assertNull(l2.getVente());
    }
}
