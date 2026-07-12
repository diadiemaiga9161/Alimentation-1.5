package com.ges.boutique.promo;

import com.ges.boutique.client.Client;
import com.ges.boutique.client.ClientRepository;
import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public Promotion creer(PromotionRequest request) {
        valider(request);
        Promotion promo = new Promotion();
        remplir(promo, request);
        return promotionRepository.save(promo);
    }

    @Override
    @Transactional
    public Promotion modifier(Long id, PromotionRequest request) {
        Promotion promo = obtenirParId(id);
        remplir(promo, request);
        return promotionRepository.save(promo);
    }

    @Override
    public Promotion obtenirParId(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Promotion non trouvée: " + id));
    }

    @Override
    public List<Promotion> obtenirToutes() {
        return promotionRepository.findAllByOrderByDateDebutDesc();
    }

    @Override
    public List<Promotion> obtenirActives() {
        return promotionRepository.findByActiveTrueAndDateFinGreaterThanEqual(LocalDate.now());
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        Promotion promo = obtenirParId(id);
        promotionRepository.delete(promo);
    }

    @Override
    public Map<String, Object> preparerMessagesWhatsApp(Long promotionId) {
        Promotion promo = obtenirParId(promotionId);
        List<Client> clients = clientRepository.findAll();

        String reduction = promo.getTypeReduction().equals("POURCENTAGE")
                ? promo.getValeurReduction().intValue() + "%"
                : promo.getValeurReduction().intValue() + " FCFA";

        String message = "🎉 *" + promo.getTitre() + "*\n\n"
                + (promo.getDescription() != null ? promo.getDescription() + "\n\n" : "")
                + "💰 Réduction : " + reduction + "\n"
                + "📅 Du " + promo.getDateDebut() + " au " + promo.getDateFin() + "\n\n"
                + "Profitez-en dès maintenant !";

        List<Map<String, String>> liens = new ArrayList<>();
        int sansNumero = 0;
        for (Client client : clients) {
            String tel = client.getNumeroTelephone();
            if (tel == null || tel.isBlank()) {
                sansNumero++;
                continue;
            }
            // Nettoyer le numéro (supprimer +, espaces, tirets)
            String telPropre = tel.replaceAll("[^0-9]", "");
            Map<String, String> lien = new HashMap<>();
            lien.put("nom", client.getNom() + " " + client.getPrenom());
            lien.put("telephone", tel);
            lien.put("url", "https://wa.me/" + telPropre + "?text=" + encodeUrl(message));
            liens.add(lien);
        }

        Map<String, Object> resultat = new HashMap<>();
        resultat.put("promotion", promo);
        resultat.put("message", message);
        resultat.put("liens", liens);
        resultat.put("totalClients", clients.size());
        resultat.put("clientsAvecTelephone", liens.size());
        resultat.put("clientsSansTelephone", sansNumero);
        return resultat;
    }

    private void valider(PromotionRequest request) {
        if (request.getTitre() == null || request.getTitre().isBlank())
            throw new IllegalArgumentException("Le titre est obligatoire");
        if (request.getDateDebut() == null || request.getDateFin() == null)
            throw new IllegalArgumentException("Les dates sont obligatoires");
        if (request.getDateFin().isBefore(request.getDateDebut()))
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        if (request.getValeurReduction() == null || request.getValeurReduction() <= 0)
            throw new IllegalArgumentException("La valeur de réduction doit être supérieure à 0");
        if (!"POURCENTAGE".equals(request.getTypeReduction()) && !"MONTANT_FIXE".equals(request.getTypeReduction()))
            throw new IllegalArgumentException("Le type de réduction doit être POURCENTAGE ou MONTANT_FIXE");
        if ("POURCENTAGE".equals(request.getTypeReduction()) && request.getValeurReduction() > 100)
            throw new IllegalArgumentException("Le pourcentage ne peut pas dépasser 100");
    }

    @Override
    public List<Promotion> obtenirPromosPourProduit(Long produitId) {
        LocalDate today = LocalDate.now();
        List<Promotion> result = new ArrayList<>();
        result.addAll(promotionRepository.findGlobalesActives(today));
        result.addAll(promotionRepository.findActivesByProduitId(produitId, today));
        return result;
    }

    private void remplir(Promotion promo, PromotionRequest request) {
        if (request.getTitre() != null) promo.setTitre(request.getTitre());
        if (request.getDescription() != null) promo.setDescription(request.getDescription());
        if (request.getDateDebut() != null) promo.setDateDebut(request.getDateDebut());
        if (request.getDateFin() != null) promo.setDateFin(request.getDateFin());
        if (request.getTypeReduction() != null) promo.setTypeReduction(request.getTypeReduction());
        if (request.getValeurReduction() != null) promo.setValeurReduction(request.getValeurReduction());
        if (request.getActive() != null) promo.setActive(request.getActive());
        if (request.getGlobale() != null) promo.setGlobale(request.getGlobale());
        if (request.getProduitIds() != null) promo.setProduitIds(request.getProduitIds());
    }

    private String encodeUrl(String text) {
        return text
                .replace(" ", "%20")
                .replace("\n", "%0A")
                .replace("*", "%2A")
                .replace("#", "%23")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace("+", "%2B")
                .replace("?", "%3F")
                .replace("🎉", "%F0%9F%8E%89")
                .replace("💰", "%F0%9F%92%B0")
                .replace("📅", "%F0%9F%93%85");
    }
}
