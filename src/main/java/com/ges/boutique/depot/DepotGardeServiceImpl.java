package com.ges.boutique.depot;

import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepotGardeServiceImpl implements DepotGardeService {

    private final DepotGardeRepository depotRepository;
    private final RetraitDepotRepository retraitRepository;
    private final DepotClientRepository depotClientRepository;

    @Override
    @Transactional
    public DepotGardeDto creerDepot(DepotGardeRequest request) {
        if (request.getMontant() == null || request.getMontant() <= 0)
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");

        DepotGarde depot = new DepotGarde();

        // Lier au client dépôt si fourni, sinon auto-créer/lier par numéro de téléphone
        if (request.getDepotClientId() != null) {
            DepotClient client = depotClientRepository.findById(request.getDepotClientId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Client dépôt introuvable"));
            depot.setDepotClient(client);
            depot.setNom(client.getNom());
            depot.setPrenom(client.getPrenom());
            depot.setNumero(client.getNumero());
        } else {
            if (request.getNom() == null || request.getNom().isBlank())
                throw new IllegalArgumentException("Le nom est obligatoire");
            if (request.getNumero() == null || request.getNumero().isBlank())
                throw new IllegalArgumentException("Le numéro de téléphone est obligatoire");

            String nom = request.getNom().trim();
            String prenom = request.getPrenom() != null ? request.getPrenom().trim() : null;
            String numero = request.getNumero().trim();
            depot.setNom(nom);
            depot.setPrenom(prenom);
            depot.setNumero(numero);

            // Auto-enregistrer la personne comme DepotClient pour la retrouver plus tard
            final String finalNom = nom;
            final String finalPrenom = prenom;
            DepotClient client = depotClientRepository.findByNumero(numero)
                    .orElseGet(() -> {
                        DepotClient nc = new DepotClient();
                        nc.setNom(finalNom);
                        nc.setPrenom(finalPrenom);
                        nc.setNumero(numero);
                        return depotClientRepository.save(nc);
                    });
            depot.setDepotClient(client);
        }

        depot.setMontantInitial(request.getMontant());
        depot.setMontantRestant(request.getMontant());
        depot.setStatut(StatutDepot.ACTIF);
        depot.setObservation(request.getObservation());

        DepotGarde saved = depotRepository.save(depot);
        log.info("Dépôt créé: {} {} - {} FCFA", saved.getPrenom(), saved.getNom(), saved.getMontantInitial());
        return DepotGardeDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public DepotGardeDto modifierDepot(Long id, DepotGardeRequest request) {
        DepotGarde depot = depotRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dépôt introuvable avec l'id: " + id));

        if (request.getNom() != null && !request.getNom().isBlank())
            depot.setNom(request.getNom().trim());
        if (request.getPrenom() != null)
            depot.setPrenom(request.getPrenom().trim());
        if (request.getNumero() != null && !request.getNumero().isBlank())
            depot.setNumero(request.getNumero().trim());
        if (request.getObservation() != null)
            depot.setObservation(request.getObservation());

        return DepotGardeDto.fromEntity(depotRepository.save(depot));
    }

    @Override
    @Transactional(readOnly = true)
    public DepotGardeDto getById(Long id) {
        return DepotGardeDto.fromEntity(depotRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dépôt introuvable avec l'id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepotGardeDto> getTous() {
        return depotRepository.findAllByOrderByDateDepotDesc().stream()
                .map(DepotGardeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepotGardeDto> getActifs() {
        return depotRepository.findByStatutOrderByDateDepotDesc(StatutDepot.ACTIF).stream()
                .map(DepotGardeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepotGardeDto> rechercher(String query) {
        return depotRepository
                .findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrNumeroContaining(query, query, query)
                .stream()
                .map(DepotGardeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DepotGardeDto effectuerRetrait(Long depotId, RetraitDepotRequest request) {
        DepotGarde depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RessourceIntrouvableException("Dépôt introuvable avec l'id: " + depotId));

        if (depot.getStatut() == StatutDepot.CLOTURE)
            throw new IllegalStateException("Ce dépôt est déjà clôturé");
        if (request.getMontant() == null || request.getMontant() <= 0)
            throw new IllegalArgumentException("Le montant du retrait doit être supérieur à 0");
        if (request.getMontant() > depot.getMontantRestant())
            throw new IllegalArgumentException(
                    "Montant demandé (" + request.getMontant() + " FCFA) supérieur au solde restant ("
                            + depot.getMontantRestant() + " FCFA)");

        RetraitDepot retrait = new RetraitDepot();
        retrait.setDepot(depot);
        retrait.setMontant(request.getMontant());
        retrait.setObservation(request.getObservation());
        retraitRepository.save(retrait);

        depot.setMontantRestant(depot.getMontantRestant() - request.getMontant());
        if (depot.getMontantRestant() == 0) {
            depot.setStatut(StatutDepot.CLOTURE);
        }
        depot.getRetraits().add(retrait);

        DepotGarde saved = depotRepository.save(depot);
        log.info("Retrait de {} FCFA sur dépôt de {} {}", request.getMontant(), depot.getPrenom(), depot.getNom());
        return DepotGardeDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public DepotGardeDto cloturerDepot(Long id) {
        DepotGarde depot = depotRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dépôt introuvable avec l'id: " + id));

        if (depot.getStatut() == StatutDepot.CLOTURE)
            throw new IllegalStateException("Ce dépôt est déjà clôturé");

        depot.setStatut(StatutDepot.CLOTURE);
        return DepotGardeDto.fromEntity(depotRepository.save(depot));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistiques() {
        List<DepotGarde> tous = depotRepository.findAllByOrderByDateDepotDesc();
        long totalActifs = tous.stream().filter(d -> d.getStatut() == StatutDepot.ACTIF).count();
        long totalClotures = tous.stream().filter(d -> d.getStatut() == StatutDepot.CLOTURE).count();
        Double totalGarde = depotRepository.getTotalMontantRestantActif();
        Double totalInitial = depotRepository.getTotalMontantInitial();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDepots", tous.size());
        stats.put("totalActifs", totalActifs);
        stats.put("totalClotures", totalClotures);
        stats.put("totalMontantGarde", totalGarde != null ? totalGarde : 0.0);
        stats.put("totalMontantInitial", totalInitial != null ? totalInitial : 0.0);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDepotGroupeDto> getDepotsGroupesParClient() {
        List<DepotGarde> actifs = depotRepository.findByStatutOrderByDateDepotAsc(StatutDepot.ACTIF);

        // Grouper par numéro de téléphone (identifiant unique du client)
        Map<String, List<DepotGarde>> parClient = new LinkedHashMap<>();
        for (DepotGarde d : actifs) {
            parClient.computeIfAbsent(d.getNumero(), k -> new ArrayList<>()).add(d);
        }

        return parClient.values().stream()
                .map(ClientDepotGroupeDto::fromDepots)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<DepotGardeDto> effectuerRetraitGlobal(RetraitGlobalRequest request) {
        if (request.getNumero() == null || request.getNumero().isBlank())
            throw new IllegalArgumentException("Le numéro du client est obligatoire");

        List<DepotGarde> depotsActifs = depotRepository
                .findByNumeroAndStatutOrderByDateDepotAsc(request.getNumero().trim(), StatutDepot.ACTIF);

        if (depotsActifs.isEmpty())
            throw new IllegalStateException("Aucun dépôt actif trouvé pour ce client");

        double totalDisponible = depotsActifs.stream().mapToDouble(DepotGarde::getMontantRestant).sum();

        // Si montant null ou 0 → retrait total
        double montantARetirer = (request.getMontant() == null || request.getMontant() <= 0)
                ? totalDisponible
                : request.getMontant();

        if (montantARetirer > totalDisponible)
            throw new IllegalArgumentException(
                    "Montant demandé (" + montantARetirer + ") supérieur au total disponible (" + totalDisponible + ")");

        List<DepotGardeDto> resultats = new ArrayList<>();
        double restantARetirer = montantARetirer;

        // Retrait du plus ancien dépôt au plus récent
        for (DepotGarde depot : depotsActifs) {
            if (restantARetirer <= 0) break;

            double aRetirerSurCeDepot = Math.min(restantARetirer, depot.getMontantRestant());

            RetraitDepot retrait = new RetraitDepot();
            retrait.setDepot(depot);
            retrait.setMontant(aRetirerSurCeDepot);
            retrait.setObservation(request.getObservation() != null ? request.getObservation() : "Retrait global");
            retraitRepository.save(retrait);

            depot.setMontantRestant(depot.getMontantRestant() - aRetirerSurCeDepot);
            if (depot.getMontantRestant() == 0) {
                depot.setStatut(StatutDepot.CLOTURE);
            }
            depot.getRetraits().add(retrait);
            DepotGarde saved = depotRepository.save(depot);
            resultats.add(DepotGardeDto.fromEntity(saved));

            restantARetirer -= aRetirerSurCeDepot;
        }

        log.info("Retrait global de {} FCFA pour le client numéro {}", montantARetirer, request.getNumero());
        return resultats;
    }
}
