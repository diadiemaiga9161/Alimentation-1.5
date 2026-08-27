package com.ges.boutique.caisse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires purs sur le calcul d'ecart de caisse (Caisse.calculerEcart,
 * verifierCaisse, mettreAJourSoldeSysteme) -- logique financiere critique
 * signalee dans l'analyse projet (CaisseServiceImpl 93 Ko, aucun test
 * unitaire existant). Aucune base de donnees ni contexte Spring necessaire.
 */
class CaisseTest {

    @Test
    void calculerEcart_positifQuandLeReelDepasseLeSysteme() {
        Caisse caisse = new Caisse();
        caisse.setSoldeSysteme(10000.0);
        caisse.setSoldeReel(10500.0);

        caisse.calculerEcart();

        assertEquals(500.0, caisse.getEcart());
    }

    @Test
    void calculerEcart_negatifQuandIlManqueDeLargent() {
        Caisse caisse = new Caisse();
        caisse.setSoldeSysteme(10000.0);
        caisse.setSoldeReel(9700.0);

        caisse.calculerEcart();

        assertEquals(-300.0, caisse.getEcart());
    }

    @Test
    void mettreAJourSoldeSysteme_alignesSoldeSystemeSurSoldeActuelEtRecalculeEcart() {
        Caisse caisse = new Caisse();
        caisse.setSoldeActuel(15000.0);
        caisse.setSoldeSysteme(12000.0);
        caisse.setSoldeReel(15000.0);

        caisse.mettreAJourSoldeSysteme();

        assertEquals(15000.0, caisse.getSoldeSysteme());
        assertEquals(0.0, caisse.getEcart()); // soldeReel == nouveau soldeSysteme
    }

    @Test
    void verifierCaisse_enregistreLeSoldeReelSaisiEtMarqueVerifiee() {
        Caisse caisse = new Caisse();
        caisse.setSoldeSysteme(20000.0);
        assertFalse(caisse.isVerifiee());

        caisse.verifierCaisse(19500.0, "admin");

        assertEquals(19500.0, caisse.getSoldeReel());
        assertTrue(caisse.isVerifiee());
        assertEquals("admin", caisse.getUtilisateurVerification());
        assertEquals(-500.0, caisse.getEcart());
        assertNotNull(caisse.getDateVerification());
    }
}
