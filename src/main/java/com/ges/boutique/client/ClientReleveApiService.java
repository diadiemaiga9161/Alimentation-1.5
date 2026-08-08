package com.ges.boutique.client;

import com.ges.boutique.caisse.OperationCaisse;
import com.ges.boutique.caisse.OperationCaisseRepository;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import com.ges.boutique.vente.LigneVente;
import com.ges.boutique.vente.RetourVente;
import com.ges.boutique.vente.RetourVenteRepository;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service JSON dédié au relevé client / "situation client" (GET /api/clients/{id}/releve).
 *
 * ATTENTION : distinct de {@link ClientReleveService} qui génère un PDF (iText) avec sa
 * propre logique de résumé simplifiée. Ne pas fusionner : formats de sortie et granularité
 * de calcul (règlements + retours mêlés chronologiquement, reliquat cumulé) trop différents.
 *
 * Sert de socle UNIQUE aux 3 fronts (Angular, Ionic, React Native) — le contrat JSON exposé
 * par {@link #genererReleve} ne doit pas être modifié sans coordination avec les 3 équipes front.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientReleveApiService {

    private final ClientRepository clientRepository;
    private final VenteRepository venteRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final RetourVenteRepository retourVenteRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Mouvement interne au niveau VENTE ENTIÈRE (avant éclatement par produit), utilisé
     * uniquement pour calculer le reliquat cumulé chronologique.
     */
    private static class Mouvement {
        LocalDateTime date;
        String type; // VENTE | VERSEMENT | RETOUR
        Vente vente;
        OperationCaisse reglement;
        RetourVente retour;
        double montantVente;   // > 0 uniquement pour VENTE
        double montantVerse;   // montant qui réduit le reliquat pour CE mouvement
        double resteAPayerApres; // calculé en cascade
    }

    public Map<String, Object> genererReleve(Long clientId, int page, int size,
                                              LocalDate dateDebut, LocalDate dateFin, String type) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));

        // findByClientId exclut déjà les ventes annulées (v.annulee IS NULL OR v.annulee = false).
        List<Vente> ventes = venteRepository.findByClientId(clientId);

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("client", clientVersMap(client));
        reponse.put("page", page);
        reponse.put("size", size);

        if (ventes.isEmpty()) {
            reponse.put("soldeActuel", 0.0);
            reponse.put("totalVentes", 0.0);
            reponse.put("totalVersements", 0.0);
            reponse.put("totalElements", 0);
            reponse.put("totalPages", 0);
            reponse.put("lignes", List.of());
            return reponse;
        }

        List<Long> venteIds = ventes.stream().map(Vente::getId).collect(Collectors.toList());

        // Règlements de crédit (acompte initial + versements ultérieurs), déjà filtrés
        // type=REGLEMENT_CREDIT et annule=false côté requête.
        List<OperationCaisse> reglements = operationCaisseRepository.findReglementsByVenteCreditIdIn(venteIds);

        // Retours liés à ces ventes.
        List<RetourVente> retours = retourVenteRepository.findByVenteIdIn(venteIds);

        // ---- 1. Construction des mouvements niveau "vente entière", tri chronologique ASC ----
        List<Mouvement> mouvements = new ArrayList<>();

        for (Vente v : ventes) {
            Mouvement m = new Mouvement();
            m.date = v.getDateVente();
            m.type = "VENTE";
            m.vente = v;
            m.montantVente = v.getMontantApresRemise() != null ? v.getMontantApresRemise()
                    : (v.getMontantTotal() != null ? v.getMontantTotal() : 0.0);

            // Portion payée AU MOMENT de la vente qui n'est PAS déjà représentée par un
            // mouvement REGLEMENT_CREDIT séparé dans la liste `reglements` :
            // - vente comptant (estCredit=false) : payée intégralement à l'instant T, aucune
            //   opération REGLEMENT_CREDIT n'est créée pour une vente comptant -> on compense
            //   ici même pour que son impact net sur le reliquat soit 0.
            // - vente à crédit : seule la part "avance client" (montantAvanceUtilise) n'est pas
            //   ré-enregistrée comme REGLEMENT_CREDIT (l'argent a déjà été encaissé lors du
            //   dépôt de l'avance, cf CaisseServiceImpl.enregistrerVenteCredit). L'éventuel
            //   acompte cash initial, lui, EST déjà repris comme REGLEMENT_CREDIT("Acompte
            //   initial") dans `reglements` -> on ne le recompte pas ici pour éviter un double
            //   comptage.
            double montantPayeALaVente;
            if (Boolean.TRUE.equals(v.getEstCredit())) {
                montantPayeALaVente = v.getMontantAvanceUtilise() != null ? v.getMontantAvanceUtilise() : 0.0;
            } else {
                montantPayeALaVente = m.montantVente;
            }
            m.montantVerse = montantPayeALaVente;
            mouvements.add(m);
        }

        for (OperationCaisse op : reglements) {
            Mouvement m = new Mouvement();
            m.date = op.getDateOperation();
            m.type = "VERSEMENT";
            m.reglement = op;
            m.montantVerse = op.getMontant() != null ? op.getMontant() : 0.0;
            mouvements.add(m);
        }

        for (RetourVente r : retours) {
            Mouvement m = new Mouvement();
            m.date = r.getDateRetour();
            m.type = "RETOUR";
            m.retour = r;
            m.montantVerse = r.getMontantTotal() != null ? r.getMontantTotal() : 0.0;
            mouvements.add(m);
        }

        mouvements.sort(Comparator.comparing(mv -> mv.date, Comparator.nullsLast(Comparator.naturalOrder())));

        // ---- 2. Calcul du reliquat cumulé sur TOUT l'historique (jamais filtré avant ce calcul) ----
        // Sert à l'affichage ligne par ligne (progression chronologique du solde) — reste
        // utile même s'il peut légèrement s'écarter du total réel sur d'anciens crédits (cf
        // point 2bis), notamment quand un paiement groupé sur plusieurs crédits ne s'est pas
        // réparti exactement en OperationCaisse par vente.
        double reliquat = 0.0;
        for (Mouvement m : mouvements) {
            reliquat = reliquat + m.montantVente - m.montantVerse;
            m.resteAPayerApres = reliquat;
        }

        // ---- 2bis. Le solde affiché en en-tête (soldeActuel) DOIT toujours correspondre
        // exactement à la liste des crédits (même source de vérité : Vente.montantRestant),
        // jamais à la reconstruction chronologique ci-dessus qui repose sur la somme des
        // OperationCaisse de type REGLEMENT_CREDIT — cette somme peut diverger de
        // Vente.montantVerse (ex: paiement groupé mal réparti entre les ventes), ce qui
        // faisait afficher un montant différent de celui de la liste des crédits.
        double soldeActuel = ventes.stream()
                .filter(v -> Boolean.TRUE.equals(v.getEstCredit()))
                .mapToDouble(v -> v.getMontantRestant() != null ? v.getMontantRestant() : 0.0)
                .sum();

        // ---- 3. Éclatement des mouvements VENTE en lignes d'affichage (une par produit) ----
        List<ClientReleveLigneDto> toutesLesLignes = new ArrayList<>();
        for (Mouvement m : mouvements) {
            switch (m.type) {
                case "VENTE" -> toutesLesLignes.addAll(eclaterVente(m));
                case "VERSEMENT" -> toutesLesLignes.add(ligneVersement(m));
                case "RETOUR" -> toutesLesLignes.add(ligneRetour(m));
                default -> { /* rien */ }
            }
        }

        // ---- 4. Totaux d'en-tête calculés sur la période filtrée (dateDebut/dateFin),
        //          mais sur tout l'historique filtré (pas seulement la page courante).
        //          Le filtre `type` n'affecte PAS ces totaux (ce sont des agrégats globaux).
        double totalVentes = 0.0;
        double totalVersements = 0.0;
        for (Mouvement m : mouvements) {
            if (!dansPeriode(m.date, dateDebut, dateFin)) continue;
            if ("VENTE".equals(m.type)) {
                totalVentes += m.montantVente;
            } else {
                totalVersements += m.montantVerse;
            }
        }

        // ---- 5. Filtre type + période sur la liste de lignes d'affichage déjà calculée ----
        List<ClientReleveLigneDto> lignesFiltrees = toutesLesLignes.stream()
                .filter(l -> dansPeriode(l.getDate(), dateDebut, dateFin))
                .filter(l -> correspondAuType(l.getType(), type))
                .sorted(Comparator.comparing(ClientReleveLigneDto::getDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        int totalElements = lignesFiltrees.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<ClientReleveLigneDto> pageLignes = lignesFiltrees.subList(fromIndex, toIndex);

        reponse.put("soldeActuel", arrondir(soldeActuel));
        reponse.put("totalVentes", arrondir(totalVentes));
        reponse.put("totalVersements", arrondir(totalVersements));
        reponse.put("totalElements", totalElements);
        reponse.put("totalPages", totalPages);
        reponse.put("lignes", pageLignes);
        return reponse;
    }

    // ==================== ÉCLATEMENT / CONSTRUCTION DES LIGNES ====================

    private List<ClientReleveLigneDto> eclaterVente(Mouvement m) {
        Vente v = m.vente;
        List<LigneVente> lignesVente = v.getLignes();
        List<ClientReleveLigneDto> resultat = new ArrayList<>();

        if (lignesVente == null || lignesVente.isEmpty()) {
            // Cas limite : vente sans ligne produit (ne devrait pas arriver) -> une seule ligne
            // "vide" pour ne pas perdre le mouvement dans l'historique.
            ClientReleveLigneDto dto = ligneBaseVente(v);
            dto.setMontantVente(arrondir(m.montantVente));
            dto.setResteAPayerApres(arrondir(m.resteAPayerApres));
            resultat.add(dto);
            return resultat;
        }

        boolean premiere = true;
        for (LigneVente lv : lignesVente) {
            ClientReleveLigneDto dto = ligneBaseVente(v);
            dto.setProduitNom(lv.getProduitNom());
            dto.setQuantite(lv.getQuantite());
            dto.setPrixUnitaire(lv.getPrixUnitaire());
            if (premiere) {
                dto.setMontantVente(arrondir(m.montantVente));
                dto.setResteAPayerApres(arrondir(m.resteAPayerApres));
                premiere = false;
            }
            resultat.add(dto);
        }
        return resultat;
    }

    private ClientReleveLigneDto ligneBaseVente(Vente v) {
        ClientReleveLigneDto dto = new ClientReleveLigneDto();
        dto.setDate(v.getDateVente());
        dto.setType("VENTE");
        dto.setReferenceVente(v.getNumeroVente());
        dto.setVenteId(v.getId());
        dto.setUtilisateurNom(getVendeurNom(v));
        return dto;
    }

    private ClientReleveLigneDto ligneVersement(Mouvement m) {
        OperationCaisse op = m.reglement;
        ClientReleveLigneDto dto = new ClientReleveLigneDto();
        dto.setDate(op.getDateOperation());
        dto.setType("VERSEMENT");
        dto.setReferenceVente(op.getVente() != null ? op.getVente().getNumeroVente() : null);
        dto.setReferenceReglement("REG-" + op.getId());
        dto.setVenteId(op.getVente() != null ? op.getVente().getId() : op.getVenteCreditId());
        dto.setMontantVersement(arrondir(m.montantVerse));
        dto.setResteAPayerApres(arrondir(m.resteAPayerApres));
        dto.setModePaiement(op.getModePaiement() != null ? op.getModePaiement().toString() : null);
        dto.setUtilisateurNom(getUtilisateurCaisseNom(op));
        return dto;
    }

    private ClientReleveLigneDto ligneRetour(Mouvement m) {
        RetourVente r = m.retour;
        ClientReleveLigneDto dto = new ClientReleveLigneDto();
        dto.setDate(r.getDateRetour());
        dto.setType("RETOUR");
        dto.setReferenceVente(r.getVente() != null ? r.getVente().getNumeroVente() : null);
        dto.setReferenceReglement(r.getNumeroRetour());
        dto.setVenteId(r.getVente() != null ? r.getVente().getId() : null);
        dto.setMontantVersement(arrondir(m.montantVerse));
        dto.setResteAPayerApres(arrondir(m.resteAPayerApres));
        dto.setUtilisateurNom(getUtilisateurRetourNom(r));
        return dto;
    }

    // ==================== HELPERS ====================

    private String getVendeurNom(Vente vente) {
        if (vente.getVendeur() == null) return "Inconnu";
        try {
            return vente.getVendeur().getNomComplet();
        } catch (Exception e) {
            return "Vendeur #" + vente.getVendeurId();
        }
    }

    private String getUtilisateurCaisseNom(OperationCaisse op) {
        try {
            Utilisateur u = op.getUtilisateur();
            return u != null ? u.getNomComplet() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getUtilisateurRetourNom(RetourVente r) {
        if (r.getUtilisateurId() == null) return null;
        try {
            return utilisateurRepository.findById(r.getUtilisateurId())
                    .map(Utilisateur::getNomComplet)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean dansPeriode(LocalDateTime date, LocalDate debut, LocalDate fin) {
        if (date == null) return debut == null && fin == null;
        if (debut != null && date.isBefore(debut.atStartOfDay())) return false;
        if (fin != null && date.isAfter(fin.atTime(LocalTime.MAX))) return false;
        return true;
    }

    private boolean correspondAuType(String ligneType, String filtreType) {
        if (filtreType == null || filtreType.isBlank()) return true;
        if ("VENTE".equalsIgnoreCase(filtreType)) return "VENTE".equals(ligneType);
        if ("VERSEMENT".equalsIgnoreCase(filtreType)) {
            return "VERSEMENT".equals(ligneType) || "RETOUR".equals(ligneType);
        }
        return true;
    }

    private Map<String, Object> clientVersMap(Client client) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", client.getId());
        map.put("nom", client.getNom());
        map.put("prenom", client.getPrenom());
        map.put("telephone", client.getNumeroTelephone());
        map.put("adresse", client.getAdresse());
        return map;
    }

    private Double arrondir(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }
}
