package com.ges.boutique.avance;

import com.ges.boutique.caisse.CaisseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvanceClientServiceImpl implements AvanceClientService {

    private final AvanceClientRepository avanceRepository;
    private final CaisseService caisseService;

    @Override
    @Transactional
    public AvanceClient enregistrerAvance(AvanceClientRequest request) {
        if (request.getMontant() == null || request.getMontant() <= 0) {
            throw new IllegalArgumentException("Le montant de l'avance doit être supérieur à 0");
        }
        if (request.getClientNom() == null || request.getClientNom().isBlank()) {
            throw new IllegalArgumentException("Le nom du client est obligatoire");
        }

        String motifCaisse = "Avance client - " + request.getClientNom().trim()
                + (request.getClientTelephone() != null ? " (" + request.getClientTelephone() + ")" : "")
                + (request.getMotif() != null ? " : " + request.getMotif() : "");

        var operationCaisse = caisseService.entreeCaisse(
                request.getMontant(),
                motifCaisse,
                request.getUtilisateurId(),
                request.getModePaiement(),
                request.getReferencePaiement()
        );

        AvanceClient avance = new AvanceClient();
        avance.setClientNom(request.getClientNom().trim());
        avance.setClientTelephone(request.getClientTelephone());
        avance.setMontant(request.getMontant());
        avance.setMontantUtilise(0.0);
        avance.setMontantDisponible(request.getMontant());
        avance.setMotif(request.getMotif());
        avance.setStatut(StatutAvance.DISPONIBLE);
        avance.setReferenceCaisseOperation(operationCaisse.getId());

        AvanceClient saved = avanceRepository.save(avance);
        log.info("Avance enregistrée : {} F pour {} - opération caisse #{}", request.getMontant(), request.getClientNom(), operationCaisse.getId());
        return saved;
    }

    @Override
    public Double getSoldeDisponible(String clientNom, String clientTelephone) {
        if (clientNom == null || clientNom.isBlank()) return 0.0;
        return avanceRepository.getSoldeDisponibleByNom(clientNom.trim());
    }

    @Override
    public List<AvanceClient> getHistoriqueParClient(String clientNom, String clientTelephone) {
        return avanceRepository.findByClientNomAndTelephone(clientNom, clientTelephone);
    }

    @Override
    public List<AvanceClient> getToutesLesAvances() {
        return avanceRepository.findAllOrderByDateDepotDesc();
    }

    @Override
    @Transactional
    public void utiliserAvance(String clientNom, Double montantAUtiliser) {
        if (montantAUtiliser == null || montantAUtiliser <= 0) return;

        Double solde = getSoldeDisponible(clientNom, null);
        if (solde < montantAUtiliser) {
            throw new IllegalStateException(
                    "Solde avance insuffisant. Disponible: " + solde + ", Demandé: " + montantAUtiliser
            );
        }

        List<AvanceClient> avancesDisponibles = avanceRepository.findAvancesDisponiblesByNom(clientNom.trim());
        double reste = montantAUtiliser;

        for (AvanceClient avance : avancesDisponibles) {
            if (reste <= 0) break;
            double utilise = Math.min(avance.getMontantDisponible(), reste);
            avance.setMontantUtilise(avance.getMontantUtilise() + utilise);
            avance.setMontantDisponible(avance.getMontantDisponible() - utilise);
            reste -= utilise;
            avanceRepository.save(avance);
        }

        log.info("Avance utilisée : {} F pour le client {}", montantAUtiliser, clientNom);
    }

    @Override
    @Transactional
    public void remettreAvance(String clientNom, Double montantARestituer) {
        if (montantARestituer == null || montantARestituer <= 0) return;

        List<AvanceClient> avancesUtilisees = avanceRepository.findAvancesUtiliseesByNom(clientNom.trim());
        double reste = montantARestituer;

        for (AvanceClient avance : avancesUtilisees) {
            if (reste <= 0) break;
            double aRestituer = Math.min(avance.getMontantUtilise(), reste);
            avance.setMontantUtilise(avance.getMontantUtilise() - aRestituer);
            avance.setMontantDisponible(avance.getMontantDisponible() + aRestituer);
            reste -= aRestituer;

            if (avance.getMontantDisponible() >= avance.getMontant()) {
                avance.setStatut(StatutAvance.DISPONIBLE);
            } else if (avance.getMontantDisponible() > 0) {
                avance.setStatut(StatutAvance.UTILISE_PARTIELLEMENT);
            }
            avanceRepository.save(avance);
        }

        log.info("Avance restituée : {} F pour le client {}", montantARestituer, clientNom);
    }
}
