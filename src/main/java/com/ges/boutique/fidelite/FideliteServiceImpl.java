package com.ges.boutique.fidelite;

import com.ges.boutique.boutique.Boutique;
import com.ges.boutique.boutique.BoutiqueRepository;
import com.ges.boutique.boutique.BoutiqueService;
import com.ges.boutique.client.Client;
import com.ges.boutique.client.ClientRepository;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.feature.CleFonctionnalite;
import com.ges.boutique.feature.FeatureToggleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FideliteServiceImpl implements FideliteService {

    private final ClientRepository clientRepository;
    private final MouvementFideliteRepository mouvementFideliteRepository;
    private final BoutiqueService boutiqueService;
    private final BoutiqueRepository boutiqueRepository;
    private final FeatureToggleService featureToggleService;

    @Override
    public void gagnerPoints(Client client, Long venteId, double montantVente) {
        // Appelé systématiquement depuis VenteServiceImpl (pas seulement quand le
        // programme est actif) : c'est ici, et seulement ici, qu'on décide de créditer
        // ou non — la vente elle-même ne doit jamais dépendre de cet état.
        if (client == null || !featureToggleService.estActive(CleFonctionnalite.PROGRAMME_FIDELITE)) return;

        Boutique boutique = boutiqueService.obtenirBoutique();
        double montantParPoint = boutique.getFideliteMontantParPoint() != null && boutique.getFideliteMontantParPoint() > 0
                ? boutique.getFideliteMontantParPoint() : 100.0;
        int points = (int) Math.floor(montantVente / montantParPoint);
        if (points <= 0) return;

        client.setPointsFidelite((client.getPointsFidelite() != null ? client.getPointsFidelite() : 0) + points);
        clientRepository.save(client);

        MouvementFidelite mouvement = new MouvementFidelite();
        mouvement.setClient(client);
        mouvement.setType(TypeMouvementFidelite.GAGNE);
        mouvement.setPoints(points);
        mouvement.setVenteId(venteId);
        mouvementFideliteRepository.save(mouvement);
    }

    @Override
    public void utiliserPoints(Long clientId, int points, Long venteId, String motif) {
        if (points <= 0) throw new IllegalArgumentException("Le nombre de points doit être positif");
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Client non trouvé: " + clientId));
        int solde = client.getPointsFidelite() != null ? client.getPointsFidelite() : 0;
        if (solde < points) {
            throw new IllegalArgumentException("Solde insuffisant : " + solde + " point(s) disponible(s)");
        }

        client.setPointsFidelite(solde - points);
        clientRepository.save(client);

        MouvementFidelite mouvement = new MouvementFidelite();
        mouvement.setClient(client);
        mouvement.setType(TypeMouvementFidelite.UTILISE);
        mouvement.setPoints(-points);
        mouvement.setVenteId(venteId);
        mouvement.setMotif(motif);
        mouvementFideliteRepository.save(mouvement);
    }

    @Override
    public void ajusterManuel(Long clientId, int delta, String motif) {
        if (delta == 0) return;
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Client non trouvé: " + clientId));
        int nouveauSolde = Math.max(0, (client.getPointsFidelite() != null ? client.getPointsFidelite() : 0) + delta);
        client.setPointsFidelite(nouveauSolde);
        clientRepository.save(client);

        MouvementFidelite mouvement = new MouvementFidelite();
        mouvement.setClient(client);
        mouvement.setType(TypeMouvementFidelite.AJUSTEMENT);
        mouvement.setPoints(delta);
        mouvement.setMotif(motif);
        mouvementFideliteRepository.save(mouvement);
    }

    @Override
    public void annulerMouvementsPourVente(Long venteId) {
        List<MouvementFidelite> mouvements = mouvementFideliteRepository.findByVenteIdAndAnnuleFalse(venteId);
        if (mouvements.isEmpty()) return;

        // Une vente peut avoir PLUSIEURS mouvements (ex: points gagnés ET utilisés sur la
        // même vente) — il faut appliquer leur effet NET en une seule fois, avec un seul
        // plancher à 0 sur le résultat final. Appliquer le plancher à chaque mouvement
        // pris isolément fausserait le résultat (ex: -185 puis +50 sur un solde de 135
        // donnerait à tort 50 au lieu de 0 si le plancher intervient entre les deux).
        Client client = mouvements.get(0).getClient();
        int solde = client.getPointsFidelite() != null ? client.getPointsFidelite() : 0;
        int effetNet = mouvements.stream().mapToInt(MouvementFidelite::getPoints).sum();
        int nouveauSolde = Math.max(0, solde - effetNet);
        client.setPointsFidelite(nouveauSolde);
        clientRepository.save(client);

        for (MouvementFidelite m : mouvements) {
            m.setAnnule(true);
            mouvementFideliteRepository.save(m);
        }
        log.info("Mouvements fidélité annulés (vente {} annulée) : client {}, solde {} -> {}",
                venteId, client.getId(), solde, nouveauSolde);
    }

    @Override
    public FideliteSoldeDto obtenirSolde(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Client non trouvé: " + clientId));
        int points = client.getPointsFidelite() != null ? client.getPointsFidelite() : 0;
        Boutique boutique = boutiqueService.obtenirBoutique();
        double pointValeur = boutique.getFidelitePointValeur() != null ? boutique.getFidelitePointValeur() : 10.0;
        return new FideliteSoldeDto(points, points * pointValeur);
    }

    @Override
    public List<MouvementFidelite> obtenirHistorique(Long clientId) {
        return mouvementFideliteRepository.findByClientIdOrderByDateMouvementDesc(clientId);
    }

    @Override
    public FideliteParametresDto obtenirParametres() {
        Boutique boutique = boutiqueService.obtenirBoutique();
        return new FideliteParametresDto(boutique.getFideliteMontantParPoint(), boutique.getFidelitePointValeur());
    }

    @Override
    public void definirParametres(FideliteParametresDto parametres) {
        Boutique boutique = boutiqueService.obtenirBoutique();
        if (parametres.getMontantParPoint() != null && parametres.getMontantParPoint() > 0) {
            boutique.setFideliteMontantParPoint(parametres.getMontantParPoint());
        }
        if (parametres.getPointValeur() != null && parametres.getPointValeur() >= 0) {
            boutique.setFidelitePointValeur(parametres.getPointValeur());
        }
        boutiqueRepository.save(boutique);
    }
}
