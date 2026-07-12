package com.ges.boutique.fournisseur;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retours-achats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RetourAchatController {

    private final RetourAchatServiceImpl retourAchatService;

    @PostMapping
    public ResponseEntity<RetourAchat> effectuerRetour(@RequestBody RetourAchatRequest request) {
        return ResponseEntity.ok(retourAchatService.effectuerRetour(request));
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    public ResponseEntity<List<RetourAchat>> getRetoursByFournisseur(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(retourAchatService.getRetoursByFournisseur(fournisseurId));
    }

    @GetMapping("/achat/{achatId}")
    public ResponseEntity<List<RetourAchat>> getRetoursByAchat(@PathVariable Long achatId) {
        return ResponseEntity.ok(retourAchatService.getRetoursByAchat(achatId));
    }

    @GetMapping
    public ResponseEntity<List<RetourAchat>> getAllRetours() {
        return ResponseEntity.ok(retourAchatService.getAllRetours());
    }
}
