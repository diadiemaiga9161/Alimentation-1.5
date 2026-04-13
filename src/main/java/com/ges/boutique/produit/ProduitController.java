package com.ges.boutique.produit;

import com.ges.boutique.fournisseur.Fournisseur;
import com.ges.boutique.fournisseur.FournisseurDto;
import com.ges.boutique.fournisseur.FournisseurRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
@Tag(name = "Produits", description = "Gestion complète des produits alimentaires et fournisseurs")
public class ProduitController {

    private final ProduitService produitService;
    private final CategorieService categorieService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir tous les produits")
    public ResponseEntity<List<Produit>> obtenirTousLesProduits() {
        return ResponseEntity.ok(produitService.obtenirTousLesProduits());
    }

    @GetMapping("/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir tous les produits au format DTO")
    public ResponseEntity<List<ProduitDto>> obtenirTousLesProduitsDto() {
        List<Produit> produits = produitService.obtenirTousLesProduits();
        return ResponseEntity.ok(produitService.convertirListeEnDto(produits));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un produit par ID")
    public ResponseEntity<Produit> obtenirProduitParId(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.obtenirProduitParId(id));
    }

    @GetMapping("/{id}/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un produit par ID au format DTO")
    public ResponseEntity<ProduitDto> obtenirProduitDtoParId(@PathVariable Long id) {
        Produit produit = produitService.obtenirProduitParId(id);
        return ResponseEntity.ok(produitService.convertirEnDto(produit));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un nouveau produit")
    public ResponseEntity<Produit> creerProduit(@RequestBody ProduitRequest request) {
        return ResponseEntity.ok(produitService.creerProduit(request));
    }

    @PostMapping("/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un nouveau produit et retourner au format DTO")
    public ResponseEntity<ProduitDto> creerProduitDto(@RequestBody ProduitRequest request) {
        Produit produit = produitService.creerProduit(request);
        return ResponseEntity.ok(produitService.convertirEnDto(produit));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un produit")
    public ResponseEntity<Produit> modifierProduit(@PathVariable Long id, @RequestBody ProduitRequest request) {
        return ResponseEntity.ok(produitService.modifierProduit(id, request));
    }

    @PutMapping("/{id}/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un produit et retourner au format DTO")
    public ResponseEntity<ProduitDto> modifierProduitDto(@PathVariable Long id, @RequestBody ProduitRequest request) {
        Produit produit = produitService.modifierProduit(id, request);
        return ResponseEntity.ok(produitService.convertirEnDto(produit));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un produit")
    public ResponseEntity<Void> supprimerProduit(@PathVariable Long id) {
        produitService.supprimerProduit(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorie/{categorieId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les produits par catégorie")
    public ResponseEntity<List<Produit>> obtenirProduitsParCategorie(@PathVariable Long categorieId) {
        return ResponseEntity.ok(produitService.obtenirProduitsParCategorie(categorieId));
    }

    @GetMapping("/categorie/{categorieId}/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les produits par catégorie au format DTO")
    public ResponseEntity<List<ProduitDto>> obtenirProduitsDtoParCategorie(@PathVariable Long categorieId) {
        List<Produit> produits = produitService.obtenirProduitsParCategorie(categorieId);
        return ResponseEntity.ok(produitService.convertirListeEnDto(produits));
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les produits par fournisseur")
    public ResponseEntity<List<Produit>> obtenirProduitsParFournisseur(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(produitService.obtenirProduitsParFournisseur(fournisseurId));
    }

    @GetMapping("/fournisseur/{fournisseurId}/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les produits par fournisseur au format DTO")
    public ResponseEntity<List<ProduitDto>> obtenirProduitsDtoParFournisseur(@PathVariable Long fournisseurId) {
        List<Produit> produits = produitService.obtenirProduitsParFournisseur(fournisseurId);
        return ResponseEntity.ok(produitService.convertirListeEnDto(produits));
    }

    @GetMapping("/recherche")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Rechercher des produits")
    public ResponseEntity<List<Produit>> rechercherProduits(@RequestParam String motCle) {
        return ResponseEntity.ok(produitService.rechercherProduits(motCle));
    }

    @GetMapping("/recherche/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Rechercher des produits au format DTO")
    public ResponseEntity<List<ProduitDto>> rechercherProduitsDto(@RequestParam String motCle) {
        List<Produit> produits = produitService.rechercherProduits(motCle);
        return ResponseEntity.ok(produitService.convertirListeEnDto(produits));
    }

    @GetMapping("/stock-faible")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les produits en stock faible")
    public ResponseEntity<List<Produit>> obtenirProduitsStockFaible() {
        return ResponseEntity.ok(produitService.obtenirProduitsStockFaible());
    }

    @GetMapping("/stock-faible/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les produits en stock faible au format DTO")
    public ResponseEntity<List<ProduitDto>> obtenirProduitsStockFaibleDto() {
        List<Produit> produits = produitService.obtenirProduitsStockFaible();
        return ResponseEntity.ok(produitService.convertirListeEnDto(produits));
    }

    @GetMapping("/perimes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les produits périmés")
    public ResponseEntity<List<Produit>> obtenirProduitsPerimes() {
        return ResponseEntity.ok(produitService.obtenirProduitsPerimes());
    }

    @GetMapping("/perimes/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les produits périmés au format DTO")
    public ResponseEntity<List<ProduitDto>> obtenirProduitsPerimesDto() {
        List<Produit> produits = produitService.obtenirProduitsPerimes();
        return ResponseEntity.ok(produitService.convertirListeEnDto(produits));
    }

    @GetMapping("/proche-peremption")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les produits proches de péremption")
    public ResponseEntity<List<Produit>> obtenirProduitsProchePeremption(
            @RequestParam(defaultValue = "7") int jours) {
        return ResponseEntity.ok(produitService.obtenirProduitsProchePeremption(jours));
    }

    @GetMapping("/proche-peremption/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les produits proches de péremption au format DTO")
    public ResponseEntity<List<ProduitDto>> obtenirProduitsProchePeremptionDto(
            @RequestParam(defaultValue = "7") int jours) {
        List<Produit> produits = produitService.obtenirProduitsProchePeremption(jours);
        return ResponseEntity.ok(produitService.convertirListeEnDto(produits));
    }

    @GetMapping("/bio")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les produits bio")
    public ResponseEntity<List<Produit>> obtenirProduitsBio() {
        return ResponseEntity.ok(produitService.obtenirProduitsBio());
    }

    @GetMapping("/bio/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les produits bio au format DTO")
    public ResponseEntity<List<ProduitDto>> obtenirProduitsBioDto() {
        List<Produit> produits = produitService.obtenirProduitsBio();
        return ResponseEntity.ok(produitService.convertirListeEnDto(produits));
    }

    @GetMapping("/code-barre/{codeBarre}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un produit par code-barre")
    public ResponseEntity<Produit> obtenirProduitParCodeBarre(@PathVariable String codeBarre) {
        return ResponseEntity.ok(produitService.obtenirProduitParCodeBarre(codeBarre));
    }

    @GetMapping("/code-barre/{codeBarre}/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un produit par code-barre au format DTO")
    public ResponseEntity<ProduitDto> obtenirProduitDtoParCodeBarre(@PathVariable String codeBarre) {
        Produit produit = produitService.obtenirProduitParCodeBarre(codeBarre);
        return ResponseEntity.ok(produitService.convertirEnDto(produit));
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques du stock")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesStock() {
        return ResponseEntity.ok(produitService.obtenirStatistiquesStock());
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir toutes les catégories")
    public ResponseEntity<List<Categorie>> obtenirToutesCategories() {
        return ResponseEntity.ok(categorieService.obtenirToutesCategories());
    }

    @GetMapping("/categories/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir toutes les catégories au format DTO")
    public ResponseEntity<List<CategorieDto>> obtenirToutesCategoriesDto() {
        List<Categorie> categories = categorieService.obtenirToutesCategories();
        return ResponseEntity.ok(categorieService.convertirListeEnDto(categories));
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir une catégorie par ID")
    public ResponseEntity<Categorie> obtenirCategorieParId(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.obtenirCategorieParId(id));
    }

    @GetMapping("/categories/{id}/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir une catégorie par ID au format DTO")
    public ResponseEntity<CategorieDto> obtenirCategorieDtoParId(@PathVariable Long id) {
        Categorie categorie = categorieService.obtenirCategorieParId(id);
        return ResponseEntity.ok(categorieService.convertirEnDto(categorie));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une nouvelle catégorie")
    public ResponseEntity<Categorie> creerCategorie(@RequestBody Categorie categorie) {
        return ResponseEntity.ok(categorieService.creerCategorie(categorie));
    }

    @PostMapping("/categories/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une nouvelle catégorie et retourner au format DTO")
    public ResponseEntity<CategorieDto> creerCategorieDto(@RequestBody Categorie categorie) {
        Categorie nouvelleCategorie = categorieService.creerCategorie(categorie);
        return ResponseEntity.ok(categorieService.convertirEnDto(nouvelleCategorie));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une catégorie")
    public ResponseEntity<Categorie> modifierCategorie(@PathVariable Long id, @RequestBody Categorie categorie) {
        return ResponseEntity.ok(categorieService.modifierCategorie(id, categorie));
    }

    @PutMapping("/categories/{id}/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une catégorie et retourner au format DTO")
    public ResponseEntity<CategorieDto> modifierCategorieDto(@PathVariable Long id, @RequestBody Categorie categorie) {
        Categorie categorieModifiee = categorieService.modifierCategorie(id, categorie);
        return ResponseEntity.ok(categorieService.convertirEnDto(categorieModifiee));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une catégorie")
    public ResponseEntity<Void> supprimerCategorie(@PathVariable Long id) {
        categorieService.supprimerCategorie(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories/existe/{nom}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Vérifier si une catégorie existe par son nom")
    public ResponseEntity<Boolean> categorieExisteParNom(@PathVariable String nom) {
        return ResponseEntity.ok(categorieService.existeParNom(nom));
    }

    @GetMapping("/fournisseurs")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir tous les fournisseurs")
    public ResponseEntity<List<Fournisseur>> obtenirTousLesFournisseurs() {
        return ResponseEntity.ok(produitService.obtenirTousLesFournisseurs());
    }

    @GetMapping("/fournisseurs/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir tous les fournisseurs au format DTO")
    public ResponseEntity<List<FournisseurDto>> obtenirTousLesFournisseursDto() {
        List<Fournisseur> fournisseurs = produitService.obtenirTousLesFournisseurs();
        return ResponseEntity.ok(produitService.convertirFournisseursEnDto(fournisseurs));
    }

    @GetMapping("/fournisseurs/actifs")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les fournisseurs actifs")
    public ResponseEntity<List<Fournisseur>> obtenirFournisseursActifs() {
        return ResponseEntity.ok(produitService.obtenirFournisseursActifs());
    }

    @GetMapping("/fournisseurs/actifs/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les fournisseurs actifs au format DTO")
    public ResponseEntity<List<FournisseurDto>> obtenirFournisseursActifsDto() {
        List<Fournisseur> fournisseurs = produitService.obtenirFournisseursActifs();
        return ResponseEntity.ok(produitService.convertirFournisseursEnDto(fournisseurs));
    }

    @GetMapping("/fournisseurs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un fournisseur par ID")
    public ResponseEntity<Fournisseur> obtenirFournisseurParId(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.obtenirFournisseurParId(id));
    }

    @GetMapping("/fournisseurs/{id}/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un fournisseur par ID au format DTO")
    public ResponseEntity<FournisseurDto> obtenirFournisseurDtoParId(@PathVariable Long id) {
        Fournisseur fournisseur = produitService.obtenirFournisseurParId(id);
        return ResponseEntity.ok(produitService.convertirFournisseurEnDto(fournisseur));
    }

    @GetMapping("/fournisseurs/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un fournisseur par code")
    public ResponseEntity<Fournisseur> obtenirFournisseurParCode(@PathVariable String code) {
        return ResponseEntity.ok(produitService.obtenirFournisseurParCode(code));
    }

    @GetMapping("/fournisseurs/code/{code}/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un fournisseur par code au format DTO")
    public ResponseEntity<FournisseurDto> obtenirFournisseurDtoParCode(@PathVariable String code) {
        Fournisseur fournisseur = produitService.obtenirFournisseurParCode(code);
        return ResponseEntity.ok(produitService.convertirFournisseurEnDto(fournisseur));
    }

    @PostMapping("/fournisseurs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un nouveau fournisseur")
    public ResponseEntity<Fournisseur> creerFournisseur(@RequestBody FournisseurRequest request) {
        return ResponseEntity.ok(produitService.creerFournisseur(request));
    }

    @PostMapping("/fournisseurs/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un nouveau fournisseur et retourner au format DTO")
    public ResponseEntity<FournisseurDto> creerFournisseurDto(@RequestBody FournisseurRequest request) {
        Fournisseur fournisseur = produitService.creerFournisseur(request);
        return ResponseEntity.ok(produitService.convertirFournisseurEnDto(fournisseur));
    }

    @PutMapping("/fournisseurs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un fournisseur")
    public ResponseEntity<Fournisseur> modifierFournisseur(@PathVariable Long id, @RequestBody FournisseurRequest request) {
        return ResponseEntity.ok(produitService.modifierFournisseur(id, request));
    }

    @PutMapping("/fournisseurs/{id}/dto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un fournisseur et retourner au format DTO")
    public ResponseEntity<FournisseurDto> modifierFournisseurDto(@PathVariable Long id, @RequestBody FournisseurRequest request) {
        Fournisseur fournisseur = produitService.modifierFournisseur(id, request);
        return ResponseEntity.ok(produitService.convertirFournisseurEnDto(fournisseur));
    }

    @DeleteMapping("/fournisseurs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un fournisseur")
    public ResponseEntity<Void> supprimerFournisseur(@PathVariable Long id) {
        produitService.supprimerFournisseur(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fournisseurs/recherche")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Rechercher des fournisseurs")
    public ResponseEntity<List<Fournisseur>> rechercherFournisseurs(@RequestParam String motCle) {
        return ResponseEntity.ok(produitService.rechercherFournisseurs(motCle));
    }

    @GetMapping("/fournisseurs/recherche/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Rechercher des fournisseurs au format DTO")
    public ResponseEntity<List<FournisseurDto>> rechercherFournisseursDto(@RequestParam String motCle) {
        List<Fournisseur> fournisseurs = produitService.rechercherFournisseurs(motCle);
        return ResponseEntity.ok(produitService.convertirFournisseursEnDto(fournisseurs));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importer des produits depuis un fichier Excel/CSV")
    public ResponseEntity<ImportResult> importerProduits(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("🔄 Controller: Début de l'importation");
            System.out.println("📁 Nom du fichier: " + file.getOriginalFilename());
            System.out.println("📊 Taille: " + file.getSize() + " bytes");

            if (file.isEmpty()) {
                System.out.println("❌ Le fichier est vide");
                return ResponseEntity.badRequest().body(new ImportResult(0, 0, 0,
                        List.of("Le fichier est vide"), List.of()));
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                return ResponseEntity.badRequest().body(new ImportResult(0, 0, 0,
                        List.of("Nom de fichier invalide"), List.of()));
            }

            String lowerCaseFileName = fileName.toLowerCase();
            if (!lowerCaseFileName.endsWith(".xlsx") &&
                    !lowerCaseFileName.endsWith(".xls") &&
                    !lowerCaseFileName.endsWith(".csv")) {
                return ResponseEntity.badRequest().body(new ImportResult(0, 0, 0,
                        List.of("Format de fichier non supporté. Utilisez .xlsx, .xls ou .csv"), List.of()));
            }

            ImportResult result = produitService.importerProduits(file);
            System.out.println("✅ Importation terminée: " + result.getSuccess() + " succès, " + result.getFailed() + " échecs");

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            System.err.println("❌ Erreur IO lors de l'importation: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ImportResult(0, 0, 0,
                    List.of("Erreur lors de la lecture du fichier: " + e.getMessage()), List.of()));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Erreur de validation: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ImportResult(0, 0, 0,
                    List.of(e.getMessage()), List.of()));
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue lors de l'importation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ImportResult(0, 0, 0,
                    List.of("Erreur lors de l'importation: " + e.getMessage()), List.of()));
        }
    }

    @GetMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Télécharger le template Excel pour l'import des produits")
    public ResponseEntity<Resource> telechargerTemplate() {
        try {
            byte[] excelData = produitService.genererTemplateExcel();

            ByteArrayResource resource = new ByteArrayResource(excelData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=template-produits-alimentaires.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Exporter tous les produits vers Excel")
    public ResponseEntity<Resource> exporterProduits() {
        try {
            byte[] excelData = produitService.exporterProduitsVersExcel();

            ByteArrayResource resource = new ByteArrayResource(excelData);

            String fileName = "produits-alimentaires-" + java.time.LocalDate.now() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/fournisseurs")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Exporter tous les fournisseurs vers Excel")
    public ResponseEntity<Resource> exporterFournisseurs() {
        try {
            byte[] excelData = produitService.exporterFournisseursVersExcel();

            ByteArrayResource resource = new ByteArrayResource(excelData);

            String fileName = "fournisseurs-" + java.time.LocalDate.now() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}