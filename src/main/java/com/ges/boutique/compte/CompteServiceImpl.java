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

        // Convertir l'enum en String
        String typeString = request.getType().toString();

        if (isDebit(typeString) && compte.getSoldeActuel() < request.getMontant() - 0.01) {
            throw new SoldeInsuffisantException("Solde bancaire insuffisant. Disponible: " + compte.getSoldeActuel());
        }

        double soldeAvant = compte.getSoldeActuel();
        appliquerOperation(compte, typeString, request.getMontant());
        compte.recalculerSolde();
        compteRepository.save(compte);

        OperationCompte op = buildOperation(compte, typeString, request.getMontant(),
                soldeAvant, compte.getSoldeActuel(), request.getMotif(), request.getReference(), request.getUtilisateurId());
        return operationCompteRepository.save(op);
    }

    @Override
    public List<OperationCompte> getHistoriqueOperations(Long compteId) {
        return operationCompteRepository.findByCompteIdOrderByDateOperationDesc(compteId);
    }

    @Override
    @Transactional
    public void debiterCompte(Long compteId, Double montant, String motif, String type, Long utilisateurId) {
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
    public void crediterCompte(Long compteId, Double montant, String motif, String type, Long utilisateurId) {
        Compte compte = getCompteById(compteId);
        double soldeAvant = compte.getSoldeActuel();
        appliquerOperation(compte, type, montant);
        compte.recalculerSolde();
        compteRepository.save(compte);
        operationCompteRepository.save(buildOperation(compte, type, montant, soldeAvant, compte.getSoldeActuel(), motif, null, utilisateurId));
        log.info("Crédit compte {}: {} F - {}", compte.getNomBanque(), montant, motif);
    }

    // ========== MÉTHODES PRIVÉES ==========

    private boolean isDebit(String type) {
        return switch (type) {
            case "RETRAIT", "CHEQUE", "FRAIS", "BON_CAISSE", "PAIEMENT_FOURNISSEUR", "AVANCE_FOURNISSEUR" -> true;
            case "VERSEMENT", "REMBOURSEMENT_ACHAT", "VIREMENT_CAISSE" -> false;
            default -> false;
        };
    }

    private void appliquerOperation(Compte compte, String type, Double montant) {
        switch (type) {
            case "VERSEMENT" -> {
                compte.setTotalVersements(compte.getTotalVersements() + montant);
                log.info("💰 VERSEMENT: +{} F sur {}", montant, compte.getNomBanque());
            }
            case "RETRAIT" -> {
                compte.setTotalRetraits(compte.getTotalRetraits() + montant);
                log.info("💳 RETRAIT: -{} F sur {}", montant, compte.getNomBanque());
            }
            case "CHEQUE" -> {
                compte.setTotalCheques(compte.getTotalCheques() + montant);
                log.info("📝 CHÈQUE: -{} F sur {}", montant, compte.getNomBanque());
            }
            case "FRAIS" -> {
                compte.setTotalFrais(compte.getTotalFrais() + montant);
                log.info("⚠️ FRAIS: -{} F sur {}", montant, compte.getNomBanque());
            }
            case "BON_CAISSE" -> {
                compte.setTotalBonsCaisse(compte.getTotalBonsCaisse() + montant);
                log.info("🎫 BON CAISSE: -{} F sur {}", montant, compte.getNomBanque());
            }
            case "PAIEMENT_FOURNISSEUR", "AVANCE_FOURNISSEUR" -> {
                compte.setTotalRetraits(compte.getTotalRetraits() + montant);
                log.info("🏦 PAIEMENT FOURNISSEUR/AVANCE: -{} F sur {}", montant, compte.getNomBanque());
            }
            case "REMBOURSEMENT_ACHAT" -> {
                compte.setTotalVersements(compte.getTotalVersements() + montant);
                log.info("🔄 REMBOURSEMENT ACHAT: +{} F sur {}", montant, compte.getNomBanque());
            }
            case "VIREMENT_CAISSE" -> {
                compte.setTotalVersements(compte.getTotalVersements() + montant);
                log.info("🏦 VIREMENT CAISSE: +{} F sur {} (versement depuis caisse)", montant, compte.getNomBanque());
            }
            default -> log.warn("Type d'opération inconnu: {}", type);
        }
    }

    private OperationCompte buildOperation(Compte compte, String type, Double montant,
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