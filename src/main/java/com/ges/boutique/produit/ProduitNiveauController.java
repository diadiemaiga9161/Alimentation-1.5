package com.ges.boutique.produit;

import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
public class ProduitNiveauController {

    private final ProduitNiveauRepository niveauRepository;
    private final ProduitRepository produitRepository;

    @GetMapping("/{produitId}/niveaux")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> getNiveaux(@PathVariable Long produitId) {
        List<ProduitNiveau> niveaux = niveauRepository.findByProduitIdOrderByOrdreAsc(produitId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("niveaux", niveaux);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{produitId}/niveaux")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> creerNiveau(
            @PathVariable Long produitId,
            @RequestBody ProduitNiveauRequest request) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé: " + produitId));
        ProduitNiveau niveau = new ProduitNiveau();
        niveau.setProduit(produit);
        niveau.setNom(request.getNom());
        niveau.setOrdre(request.getOrdre());
        niveau.setFacteur(request.getFacteur());
        niveau.setPrixAchat(request.getPrixAchat());
        niveau.setPrixVente(request.getPrixVente());
        ProduitNiveau saved = niveauRepository.save(niveau);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("niveau", saved);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/niveaux/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> modifierNiveau(
            @PathVariable Long id,
            @RequestBody ProduitNiveauRequest request) {
        ProduitNiveau niveau = niveauRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Niveau non trouvé: " + id));
        if (request.getNom() != null) niveau.setNom(request.getNom());
        if (request.getOrdre() != null) niveau.setOrdre(request.getOrdre());
        if (request.getFacteur() != null) niveau.setFacteur(request.getFacteur());
        if (request.getPrixAchat() != null) niveau.setPrixAchat(request.getPrixAchat());
        if (request.getPrixVente() != null) niveau.setPrixVente(request.getPrixVente());
        if (request.getStock() != null) niveau.setStock(request.getStock());
        ProduitNiveau saved = niveauRepository.save(niveau);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("niveau", saved);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/niveaux/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> supprimerNiveau(@PathVariable Long id) {
        niveauRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Niveau non trouvé: " + id));
        niveauRepository.deleteById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Niveau supprimé");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/niveaux/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> ajusterStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        ProduitNiveau niveau = niveauRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Niveau non trouvé: " + id));
        Integer newStock = body.get("stock");
        if (newStock == null || newStock < 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Stock invalide"));
        }
        niveau.setStock(newStock);
        ProduitNiveau saved = niveauRepository.save(niveau);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("niveau", saved);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/niveaux/{id}/decomposer")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Transactional
    public ResponseEntity<Map<String, Object>> decomposer(@PathVariable Long id) {
        ProduitNiveau target = niveauRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Niveau non trouvé: " + id));

        Produit produit = target.getProduit();
        List<ProduitNiveau> niveaux = niveauRepository.findByProduitIdOrderByOrdreAsc(produit.getId());

        int idx = -1;
        for (int i = 0; i < niveaux.size(); i++) {
            if (niveaux.get(i).getId().equals(id)) { idx = i; break; }
        }

        if (idx == 0) {
            // Parent direct = produit
            if (produit.getQuantite() == null || produit.getQuantite() < 1) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Stock " + produit.getNom() + " insuffisant pour décomposer en " + target.getNom()
                ));
            }
            produit.setQuantite(produit.getQuantite() - 1);
            produitRepository.save(produit);
        } else {
            // Parent direct = niveau supérieur immédiat uniquement
            ProduitNiveau parent = niveaux.get(idx - 1);
            int parentStock = parent.getStock() != null ? parent.getStock() : 0;
            if (parentStock < 1) {
                String grandParentNom = (idx == 1) ? produit.getNom() : niveaux.get(idx - 2).getNom();
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Stock " + parent.getNom() + " épuisé. Décomposez d'abord " +
                                grandParentNom + " → " + parent.getNom()
                ));
            }
            parent.setStock(parentStock - 1);
            niveauRepository.save(parent);
        }

        int newStock = (target.getStock() != null ? target.getStock() : 0) + target.getFacteur();
        target.setStock(newStock);
        niveauRepository.save(target);

        List<ProduitNiveau> updatedNiveaux = niveauRepository.findByProduitIdOrderByOrdreAsc(produit.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Décomposition effectuée : 1 " + (idx == 0 ? produit.getNom() : niveaux.get(idx - 1).getNom()) + " → " + target.getFacteur() + " " + target.getNom());
        response.put("niveaux", updatedNiveaux);
        response.put("produitQuantite", produit.getQuantite());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{produitId}/niveaux")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> supprimerTousNiveaux(@PathVariable Long produitId) {
        niveauRepository.deleteByProduitId(produitId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Tous les niveaux supprimés");
        return ResponseEntity.ok(response);
    }
}
