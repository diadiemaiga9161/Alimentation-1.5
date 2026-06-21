package com.ges.boutique.produit;

import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
