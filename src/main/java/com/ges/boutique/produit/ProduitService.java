package com.ges.boutique.produit;

import com.ges.boutique.fournisseur.Fournisseur;
import com.ges.boutique.fournisseur.FournisseurDto;
import com.ges.boutique.fournisseur.FournisseurRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ProduitService {

    Produit creerProduit(ProduitRequest request);
    Produit modifierProduit(Long id, ProduitRequest request);
    void supprimerProduit(Long id);
    Produit obtenirProduitParId(Long id);
    List<Produit> obtenirTousLesProduits();
    List<Produit> obtenirProduitsParCategorie(Long categorieId);
    List<Produit> obtenirProduitsParFournisseur(Long fournisseurId);
    List<Produit> rechercherProduits(String motCle);
    List<Produit> obtenirProduitsStockFaible();
    List<Produit> obtenirProduitsPerimes();
    List<Produit> obtenirProduitsProchePeremption(int jours);
    List<Produit> obtenirProduitsBio();
    Map<String, Object> obtenirStatistiquesStock();
    Produit obtenirProduitParCodeBarre(String codeBarre);

    Fournisseur creerFournisseur(FournisseurRequest request);
    Fournisseur modifierFournisseur(Long id, FournisseurRequest request);
    void supprimerFournisseur(Long id);
    Fournisseur obtenirFournisseurParId(Long id);
    List<Fournisseur> obtenirTousLesFournisseurs();
    List<Fournisseur> rechercherFournisseurs(String motCle);
    List<Fournisseur> obtenirFournisseursActifs();
    Fournisseur obtenirFournisseurParCode(String code);
    int compterProduitsParFournisseur(Long fournisseurId);

    ImportResult importerProduits(MultipartFile file) throws IOException;
    byte[] genererTemplateExcel() throws IOException;
    byte[] exporterProduitsVersExcel() throws IOException;
    byte[] exporterFournisseursVersExcel() throws IOException;

    ProduitDto convertirEnDto(Produit produit);
    List<ProduitDto> convertirListeEnDto(List<Produit> produits);
    FournisseurDto convertirFournisseurEnDto(Fournisseur fournisseur);
    List<FournisseurDto> convertirFournisseursEnDto(List<Fournisseur> fournisseurs);
}