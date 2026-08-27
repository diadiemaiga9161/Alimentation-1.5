package com.ges.boutique.caisse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests sur getStatistiquesParPeriode -- correctif de l'audit comptable :
 * soldeNetPeriode excluait silencieusement les remboursements de retour, les
 * virements vers la banque, les dépenses et tout type d'opération non listé
 * explicitement dans le switch, faussant le "solde net" affiché par rapport au
 * vrai mouvement de caisse. Le nouveau calcul (delta soldeApres - soldeAvant par
 * opération, sur la liste NON filtrée) doit rester correct quel que soit le type.
 */
@ExtendWith(MockitoExtension.class)
class CaisseServiceImplStatistiquesTest {

    @Mock private CaisseRepository caisseRepository;
    @Mock private OperationCaisseRepository operationRepository;
    @Mock private com.ges.boutique.utilisateur.UtilisateurRepository utilisateurRepository;
    @Mock private com.ges.boutique.vente.VenteRepository venteRepository;
    @Mock private com.ges.boutique.facture.FactureRepository factureRepository;
    @Mock private com.ges.boutique.facture.LigneFactureRepository ligneFactureRepository;
    @Mock private com.ges.boutique.client.ClientRepository clientRepository;
    @Mock private com.ges.boutique.produit.ProduitRepository produitRepository;
    @Mock private com.ges.boutique.compte.CompteRepository compteRepository;
    @Mock private com.ges.boutique.compte.OperationCompteRepository operationCompteRepository;
    @Mock private TransfertCaisseBanqueRepository transfertRepository;
    @Mock private com.ges.boutique.config.NotificationService notificationService;

    @InjectMocks
    private CaisseServiceImpl caisseService;

    private OperationCaisse op(TypeOperationCaisse type, double soldeAvant, double soldeApres, boolean venteAnnulee) {
        OperationCaisse o = new OperationCaisse();
        o.setType(type);
        o.setMontant(Math.abs(soldeApres - soldeAvant));
        o.setSoldeAvant(soldeAvant);
        o.setSoldeApres(soldeApres);
        o.setDateOperation(LocalDateTime.now());
        o.setVenteAnnulee(venteAnnulee);
        return o;
    }

    @Test
    void soldeNetPeriode_inclutLesRemboursementsRetourEtLesVirementsBanque() {
        // Vente comptant : +10000. Remboursement retour (avant ce fix : ignoré) : -3000.
        // Virement vers la banque (avant ce fix : ignoré) : -2000.
        List<OperationCaisse> operations = List.of(
                op(TypeOperationCaisse.VENTE_COMPTANT, 0, 10000, false),
                op(TypeOperationCaisse.REMBOURSEMENT_RETOUR, 10000, 7000, false),
                op(TypeOperationCaisse.VIREMENT_BANQUE, 7000, 5000, false)
        );
        when(operationRepository.findOperationsParPeriode(any(), any())).thenReturn(operations);

        Map<String, Object> stats = caisseService.getStatistiquesParPeriode(LocalDate.now(), LocalDate.now());

        // Vrai mouvement de caisse : 0 -> 5000, soit +5000 net.
        // Avant le correctif, soldeNetPeriode aurait affiché 10000 (remboursement et
        // virement tous deux exclus du total des sorties utilisé pour le calcul).
        assertEquals(5000.0, (Double) stats.get("soldeNetPeriode"));
    }

    @Test
    void soldeNetPeriode_venteEtAnnulationDansLaMemePeriode_seNeutralisent() {
        // La vente d'origine est marquée venteAnnulee=true rétroactivement par
        // l'annulation ; elle doit quand même compter dans le delta réel (liste non
        // filtrée), sinon le solde net serait faussé de -montant au lieu de 0.
        List<OperationCaisse> operations = List.of(
                op(TypeOperationCaisse.VENTE_COMPTANT, 0, 8000, true),
                op(TypeOperationCaisse.ANNULATION_VENTE, 8000, 0, false)
        );
        when(operationRepository.findOperationsParPeriode(any(), any())).thenReturn(operations);

        Map<String, Object> stats = caisseService.getStatistiquesParPeriode(LocalDate.now(), LocalDate.now());

        assertEquals(0.0, (Double) stats.get("soldeNetPeriode"));
    }
}
