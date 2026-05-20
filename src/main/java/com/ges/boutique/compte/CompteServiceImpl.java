package com.ges.boutique.compte;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.SoldeInsuffisantException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompteServiceImpl implements CompteService {

    private final CompteRepository compteRepository;
    private final OperationCompteRepository operationCompteRepository;

    @Override
    @Transactional
    public Compte creerCompte(CompteRequest request) {
        Compte compte = new Compte();
        compte.setNomBanque(request.getNomBanque());
        compte.setNumeroCompte(request.getNumeroCompte());
        compte.setAgence(request.getAgence());
        compte.setTitulaire(request.getTitulaire());
        compte.setSoldeInitial(request.getSoldeInitial() != null ? request.getSoldeInitial() : 0.0);
        compte.setTotalVersements(0.0);
        compte.setTotalRetraits(0.0);
        compte.setTotalCheques(0.0);
        compte.setTotalFrais(0.0);
        compte.setTotalBonsCaisse(0.0);
        compte.setDescription(request.getDescription());
        compte.setActif(true);
        compte.recalculerSolde();
        Compte saved = compteRepository.save(compte);
        log.info("Compte bancaire créé: {} - solde initial: {}", saved.getNomBanque(), saved.getSoldeInitial());
        return saved;
    }

    @Override
    @Transactional
    public Compte modifierCompte(Long id, CompteRequest request) {
        Compte compte = getCompteById(id);
        compte.setNomBanque(request.getNomBanque());
        compte.setNumeroCompte(request.getNumeroCompte());
        compte.setAgence(request.getAgence());
        compte.setTitulaire(request.getTitulaire());
        if (request.getSoldeInitial() != null) {
            compte.setSoldeInitial(request.getSoldeInitial());
        }
        compte.setDescription(request.getDescription());
        compte.recalculerSolde();
        return compteRepository.save(compte);
    }

    @Override
    public List<Compte> getTousLesComptes() {
        return compteRepository.findAllByOrderByNomBanqueAsc();
    }

    @Override
    public Compte getCompteById(Long id) {
        return compteRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte bancaire introuvable : " + id));
    }

    @Override
    @Transactional
    public OperationCompte enregistrerOperation(OperationCompteRequest request) {
        Compte compte = getCompteById(request.getCompteId());

        if (isDebit(request.getType()) && compte.getSoldeActuel() < request.getMontant() - 0.01) {
            throw new SoldeInsuffisantException("Solde bancaire insuffisant. Disponible: " + compte.getSoldeActuel());
        }

        double soldeAvant = compte.getSoldeActuel();
        appliquerOperation(compte, request.getType(), request.getMontant());
        compte.recalculerSolde();
        compteRepository.save(compte);

        OperationCompte op = buildOperation(compte, request.getType(), request.getMontant(),
                soldeAvant, compte.getSoldeActuel(), request.getMotif(), request.getReference(), request.getUtilisateurId());
        return operationCompteRepository.save(op);
    }

    @Override
    public List<OperationCompte> getHistoriqueOperations(Long compteId) {
        return operationCompteRepository.findByCompteIdOrderByDateOperationDesc(compteId);
    }

    @Override
    @Transactional
    public void debiterCompte(Long compteId, Double montant, String motif, TypeOperationCompte type, Long utilisateurId) {
        Compte compte = getCompteById(compteId);
        if (compte.getSoldeActuel() < montant - 0.01) {
            throw new SoldeInsuffisantException(
                    "Solde bancaire insuffisant pour payer " + montant + " F. Disponible: " + compte.getSoldeActuel() + " F");
        }
        double soldeAvant = compte.getSoldeActuel();
        appliquerOperation(compte, type, montant);
        compte.recalculerSolde();
        compteRepository.save(compte);
        operationCompteRepository.save(buildOperation(compte, type, montant, soldeAvant, compte.getSoldeActuel(), motif, null, utilisateurId));
        log.info("Débit compte {}: {} F - {}", compte.getNomBanque(), montant, motif);
    }

    @Override
    @Transactional
    public void crediterCompte(Long compteId, Double montant, String motif, TypeOperationCompte type, Long utilisateurId) {
        Compte compte = getCompteById(compteId);
        double soldeAvant = compte.getSoldeActuel();
        appliquerOperation(compte, type, montant);
        compte.recalculerSolde();
        compteRepository.save(compte);
        operationCompteRepository.save(buildOperation(compte, type, montant, soldeAvant, compte.getSoldeActuel(), motif, null, utilisateurId));
        log.info("Crédit compte {}: {} F - {}", compte.getNomBanque(), montant, motif);
    }

    private boolean isDebit(TypeOperationCompte type) {
        return switch (type) {
            case RETRAIT, CHEQUE, FRAIS, BON_CAISSE, PAIEMENT_FOURNISSEUR, AVANCE_FOURNISSEUR -> true;
            default -> false;
        };
    }

    private void appliquerOperation(Compte compte, TypeOperationCompte type, Double montant) {
        switch (type) {
            case VERSEMENT -> compte.setTotalVersements(compte.getTotalVersements() + montant);
            case RETRAIT -> compte.setTotalRetraits(compte.getTotalRetraits() + montant);
            case CHEQUE -> compte.setTotalCheques(compte.getTotalCheques() + montant);
            case FRAIS -> compte.setTotalFrais(compte.getTotalFrais() + montant);
            case BON_CAISSE -> compte.setTotalBonsCaisse(compte.getTotalBonsCaisse() + montant);
            case PAIEMENT_FOURNISSEUR, AVANCE_FOURNISSEUR -> compte.setTotalRetraits(compte.getTotalRetraits() + montant);
        }
    }

    private OperationCompte buildOperation(Compte compte, TypeOperationCompte type, Double montant,
                                            double soldeAvant, double soldeApres, String motif, String reference, Long utilisateurId) {
        OperationCompte op = new OperationCompte();
        op.setCompte(compte);
        op.setType(type);
        op.setMontant(montant);
        op.setSoldeAvant(soldeAvant);
        op.setSoldeApres(soldeApres);
        op.setMotif(motif);
        op.setReference(reference);
        op.setUtilisateurId(utilisateurId);
        return op;
    }
}
