package com.ges.boutique.vente;

import com.ges.boutique.avance.AvanceClientService;
import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.inventaire.InventaireService;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests sur le calcul du bénéfice retiré du CA lors d'un retour de marchandise
 * (RetourVenteServiceImpl.calculerBeneficeRetourneLigne) -- correctif de l'audit
 * comptable : le CA/bénéfice affichés dans les rapports comptaient auparavant la
 * marchandise retournée comme toujours vendue.
 */
@ExtendWith(MockitoExtension.class)
class RetourVenteServiceImplTest {

    @Mock private VenteRepository venteRepository;
    @Mock private LigneVenteRepository ligneVenteRepository;
    @Mock private ProduitRepository produitRepository;
    @Mock private RetourVenteRepository retourVenteRepository;
    @Mock private CaisseService caisseService;
    @Mock private AvanceClientService avanceClientService;
    @Mock private InventaireService inventaireService;

    @InjectMocks
    private RetourVenteServiceImpl retourVenteService;

    private Produit produit(double prixAchatActuel) {
        Produit p = new Produit();
        p.setId(1L);
        p.setPrixAchat(prixAchatActuel);
        return p;
    }

    @Test
    void beneficeRetourne_utiliseLeBeneficeReelDeLaLigneVenteOrigine_pasLePrixCatalogueActuel() {
        // Ligne vendue à l'origine : 5 unités, bénéfice total 2500 F (500 F/unité)
        LigneVente ligneOrigine = new LigneVente();
        ligneOrigine.setQuantite(5);
        ligneOrigine.setBenefice(2500.0);
        when(ligneVenteRepository.findById(42L)).thenReturn(Optional.of(ligneOrigine));

        RetourVenteRequest.LigneRetourVenteRequest req = new RetourVenteRequest.LigneRetourVenteRequest();
        req.setLigneVenteId(42L);
        req.setQuantiteRetournee(2);
        req.setPrixUnitaire(1000.0);

        // Le prix d'achat catalogue a changé depuis (ex: 900 F aujourd'hui) -- ne doit PAS être utilisé
        Produit produitAujourdhui = produit(900.0);

        double benefice = retourVenteService.calculerBeneficeRetourneLigne(req, produitAujourdhui);

        // 500 F/unité (coût réel à la vente) * 2 unités retournées = 1000 F
        assertEquals(1000.0, benefice);
    }

    @Test
    void beneficeRetourne_repliSurLaMargeCatalogueActuelle_siLigneVenteIdAbsent() {
        RetourVenteRequest.LigneRetourVenteRequest req = new RetourVenteRequest.LigneRetourVenteRequest();
        req.setLigneVenteId(null);
        req.setQuantiteRetournee(3);
        req.setPrixUnitaire(1000.0);

        Produit produitAujourdhui = produit(700.0);

        double benefice = retourVenteService.calculerBeneficeRetourneLigne(req, produitAujourdhui);

        // Repli : (1000 - 700) * 3 = 900
        assertEquals(900.0, benefice);
    }

    @Test
    void beneficeRetourne_repliSurLaMargeCatalogueActuelle_siLigneVenteIntrouvable() {
        when(ligneVenteRepository.findById(99L)).thenReturn(Optional.empty());

        RetourVenteRequest.LigneRetourVenteRequest req = new RetourVenteRequest.LigneRetourVenteRequest();
        req.setLigneVenteId(99L);
        req.setQuantiteRetournee(1);
        req.setPrixUnitaire(500.0);

        Produit produitAujourdhui = produit(300.0);

        double benefice = retourVenteService.calculerBeneficeRetourneLigne(req, produitAujourdhui);

        assertEquals(200.0, benefice);
    }

    @Test
    void beneficeRetourne_quantiteNulleOuNegative_retourneZero() {
        RetourVenteRequest.LigneRetourVenteRequest req = new RetourVenteRequest.LigneRetourVenteRequest();
        req.setQuantiteRetournee(0);

        double benefice = retourVenteService.calculerBeneficeRetourneLigne(req, produit(500.0));

        assertEquals(0.0, benefice);
    }
}
