package com.ges.boutique.produit;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.fournisseur.Fournisseur;
import com.ges.boutique.fournisseur.FournisseurRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final FournisseurRepository fournisseurRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ImportResult importerProduits(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> details = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int totalCount = 0;

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }

        InputStream inputStream = file.getInputStream();

        if (fileName.toLowerCase().endsWith(".csv")) {
            List<Map<String, String>> csvData = lireCSV(inputStream);
            totalCount = csvData.size();

            for (int i = 0; i < csvData.size(); i++) {
                try {
                    Map<String, String> ligne = csvData.get(i);
                    importerLigneProduit(ligne, i + 1);
                    successCount++;
                    details.add(String.format("Ligne %d: Produit '%s' importé avec succès",
                            i + 1, ligne.get("Nom")));
                } catch (Exception e) {
                    failedCount++;
                    errors.add(String.format("Ligne %d: %s", i + 1, e.getMessage()));
                }
            }
        } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
            try (Workbook workbook = WorkbookFactory.create(inputStream)) {
                Sheet sheet = workbook.getSheetAt(0);
                totalCount = sheet.getPhysicalNumberOfRows() - 1;

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        try {
                            Map<String, String> ligne = lireLigneExcel(row);
                            importerLigneProduit(ligne, i);
                            successCount++;
                            details.add(String.format("Ligne %d: Produit '%s' importé avec succès",
                                    i, ligne.get("Nom")));
                        } catch (Exception e) {
                            failedCount++;
                            errors.add(String.format("Ligne %d: %s", i, e.getMessage()));
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Format de fichier non supporté. Utilisez .xlsx, .xls ou .csv");
        }

        return new ImportResult(successCount, failedCount, totalCount, errors, details);
    }

    private List<Map<String, String>> lireCSV(InputStream inputStream) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String ligne;
            String[] enTetes = null;
            int ligneNumero = 0;

            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;

                String[] valeurs = ligne.split(",", -1);

                if (ligneNumero == 0) {
                    enTetes = new String[valeurs.length];
                    for (int i = 0; i < valeurs.length; i++) {
                        enTetes[i] = valeurs[i].trim();
                    }
                } else {
                    if (enTetes == null) {
                        throw new IOException("Fichier CSV sans en-tête");
                    }

                    Map<String, String> map = new HashMap<>();
                    for (int i = 0; i < Math.min(enTetes.length, valeurs.length); i++) {
                        map.put(enTetes[i], valeurs[i].trim());
                    }
                    data.add(map);
                }
                ligneNumero++;
            }
        }
        return data;
    }

    private Map<String, String> lireLigneExcel(Row row) {
        Map<String, String> ligne = new HashMap<>();

        Row headerRow = row.getSheet().getRow(0);
        Map<Integer, String> colonnesIndex = new HashMap<>();

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String valeur = "";
                if (cell.getCellType() == CellType.STRING) {
                    valeur = cell.getStringCellValue().trim();
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    valeur = String.valueOf(cell.getNumericCellValue());
                } else if (cell.getCellType() == CellType.BOOLEAN) {
                    valeur = String.valueOf(cell.getBooleanCellValue());
                } else if (cell.getCellType() == CellType.FORMULA) {
                    try {
                        valeur = cell.getStringCellValue();
                    } catch (Exception e) {
                        valeur = cell.getCellFormula();
                    }
                }
                colonnesIndex.put(i, valeur);
            }
        }

        for (Map.Entry<Integer, String> entry : colonnesIndex.entrySet()) {
            int index = entry.getKey();
            String nomColonne = entry.getValue();
            Cell cell = row.getCell(index);

            if (cell != null) {
                String valeur = "";
                switch (cell.getCellType()) {
                    case STRING:
                        valeur = cell.getStringCellValue().trim();
                        break;
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            valeur = cell.getDateCellValue().toString();
                        } else {
                            valeur = String.valueOf(cell.getNumericCellValue());
                            if (valeur.endsWith(".0")) {
                                valeur = valeur.substring(0, valeur.length() - 2);
                            }
                        }
                        break;
                    case BOOLEAN:
                        valeur = String.valueOf(cell.getBooleanCellValue());
                        break;
                    case FORMULA:
                        try {
                            valeur = cell.getStringCellValue();
                        } catch (Exception e) {
                            try {
                                valeur = String.valueOf(cell.getNumericCellValue());
                            } catch (Exception ex) {
                                valeur = cell.getCellFormula();
                            }
                        }
                        break;
                    default:
                        valeur = "";
                }
                ligne.put(nomColonne, valeur);
            } else {
                ligne.put(nomColonne, "");
            }
        }

        return ligne;
    }

    private void importerLigneProduit(Map<String, String> ligne, int numeroLigne) {
        if (!ligne.containsKey("Nom") || ligne.get("Nom") == null || ligne.get("Nom").trim().isEmpty()) {
            throw new IllegalArgumentException("Nom du produit manquant");
        }

        if (!ligne.containsKey("Categorie") || ligne.get("Categorie") == null || ligne.get("Categorie").trim().isEmpty()) {
            throw new IllegalArgumentException("Catégorie manquante");
        }

        if (!ligne.containsKey("PrixAchat") || ligne.get("PrixAchat") == null || ligne.get("PrixAchat").trim().isEmpty()) {
            throw new IllegalArgumentException("Prix d'achat manquant");
        }

        if (!ligne.containsKey("PrixVente") || ligne.get("PrixVente") == null || ligne.get("PrixVente").trim().isEmpty()) {
            throw new IllegalArgumentException("Prix de vente manquant");
        }

        if (!ligne.containsKey("Quantite") || ligne.get("Quantite") == null || ligne.get("Quantite").trim().isEmpty()) {
            throw new IllegalArgumentException("Quantité manquante");
        }

        String nomCategorie = ligne.get("Categorie").trim();
        Categorie categorie = categorieRepository.findByNom(nomCategorie).orElse(null);

        if (categorie == null) {
            categorie = new Categorie();
            categorie.setNom(nomCategorie);
            categorie.setDescription("Catégorie importée depuis Excel");
            categorie = categorieRepository.save(categorie);
        }

        Fournisseur fournisseur = null;
        if (ligne.containsKey("Fournisseur") && ligne.get("Fournisseur") != null && !ligne.get("Fournisseur").trim().isEmpty()) {
            String nomFournisseur = ligne.get("Fournisseur").trim();
            fournisseur = fournisseurRepository.findByNom(nomFournisseur).orElse(null);

            if (fournisseur == null) {
                fournisseur = new Fournisseur();
                fournisseur.setNom(nomFournisseur);
                fournisseur.setCode("IMP" + System.currentTimeMillis());
                fournisseur = fournisseurRepository.save(fournisseur);
            }
        }

        Produit produit = new Produit();
        produit.setNom(ligne.get("Nom").trim());

        if (ligne.containsKey("Description") && ligne.get("Description") != null) {
            produit.setDescription(ligne.get("Description").trim());
        }

        produit.setCategorie(categorie);
        produit.setFournisseur(fournisseur);

        try {
            produit.setPrixAchat(Double.parseDouble(ligne.get("PrixAchat").replace(",", ".")));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prix d'achat invalide: " + ligne.get("PrixAchat"));
        }

        try {
            produit.setPrixVente(Double.parseDouble(ligne.get("PrixVente").replace(",", ".")));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prix de vente invalide: " + ligne.get("PrixVente"));
        }

        try {
            produit.setQuantite(Integer.parseInt(ligne.get("Quantite").replace(",", ".").split("\\.")[0]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantité invalide: " + ligne.get("Quantite"));
        }

        if (ligne.containsKey("SeuilAlerte") && ligne.get("SeuilAlerte") != null && !ligne.get("SeuilAlerte").trim().isEmpty()) {
            try {
                produit.setSeuilAlerte(Integer.parseInt(ligne.get("SeuilAlerte").replace(",", ".").split("\\.")[0]));
            } catch (NumberFormatException e) {
                produit.setSeuilAlerte(10);
            }
        } else {
            produit.setSeuilAlerte(10);
        }

        if (ligne.containsKey("CodeBarre") && ligne.get("CodeBarre") != null && !ligne.get("CodeBarre").trim().isEmpty()) {
            produit.setCodeBarre(ligne.get("CodeBarre").trim());
        }

        if (ligne.containsKey("DatePeremption") && ligne.get("DatePeremption") != null && !ligne.get("DatePeremption").trim().isEmpty()) {
            try {
                produit.setDatePeremption(LocalDate.parse(ligne.get("DatePeremption"), DATE_FORMATTER));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Date de péremption invalide: " + ligne.get("DatePeremption"));
            }
        }

        if (ligne.containsKey("LotNumber") && ligne.get("LotNumber") != null) {
            produit.setLotNumber(ligne.get("LotNumber").trim());
        }

        if (ligne.containsKey("ConditionsStockage") && ligne.get("ConditionsStockage") != null) {
            produit.setConditionsStockage(ligne.get("ConditionsStockage").trim());
        }

        if (ligne.containsKey("PoidsVolume") && ligne.get("PoidsVolume") != null && !ligne.get("PoidsVolume").trim().isEmpty()) {
            try {
                produit.setPoidsVolume(Double.parseDouble(ligne.get("PoidsVolume").replace(",", ".")));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Poids/Volume invalide: " + ligne.get("PoidsVolume"));
            }
        }

        if (ligne.containsKey("UniteMesure") && ligne.get("UniteMesure") != null) {
            produit.setUniteMesure(ligne.get("UniteMesure").trim());
        }

        if (ligne.containsKey("Bio") && ligne.get("Bio") != null) {
            String bio = ligne.get("Bio").trim().toLowerCase();
            produit.setBio(bio.equals("oui") || bio.equals("true") || bio.equals("1") || bio.equals("yes"));
        }

        if (ligne.containsKey("Origine") && ligne.get("Origine") != null) {
            produit.setOrigine(ligne.get("Origine").trim());
        }

        if (produit.getPrixAchat() <= 0) {
            throw new IllegalArgumentException("Le prix d'achat doit être supérieur à 0");
        }

        if (produit.getPrixVente() <= 0) {
            throw new IllegalArgumentException("Le prix de vente doit être supérieur à 0");
        }

        if (produit.getQuantite() < 0) {
            throw new IllegalArgumentException("La quantité ne peut pas être négative");
        }

        if (produit.getPrixVente() < produit.getPrixAchat()) {
            throw new IllegalArgumentException("Le prix de vente doit être supérieur ou égal au prix d'achat");
        }

        boolean produitExiste = produitRepository.existsByNomAndCategorieId(
                produit.getNom(),
                categorie.getId()
        );

        if (produitExiste) {
            throw new IllegalArgumentException("Le produit '" + produit.getNom() + "' existe déjà dans cette catégorie");
        }

        produitRepository.save(produit);
    }

    public byte[] genererTemplateExcel() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template Produits");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] enTetes = {"Nom", "Categorie", "Fournisseur", "Description", "PrixAchat",
                    "PrixVente", "Quantite", "SeuilAlerte", "CodeBarre", "DatePeremption",
                    "LotNumber", "ConditionsStockage", "PoidsVolume", "UniteMesure",
                    "Bio", "Origine"};

            for (int i = 0; i < enTetes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(enTetes[i]);
                cell.setCellStyle(headerStyle);
            }

            Row exempleRow1 = sheet.createRow(1);
            exempleRow1.createCell(0).setCellValue("Lait Demi-écrémé");
            exempleRow1.createCell(1).setCellValue("Produits Laitiers");
            exempleRow1.createCell(2).setCellValue("Laiterie Moderne");
            exempleRow1.createCell(3).setCellValue("Lait 1L");
            exempleRow1.createCell(4).setCellValue(0.80);
            exempleRow1.createCell(5).setCellValue(1.20);
            exempleRow1.createCell(6).setCellValue(50);
            exempleRow1.createCell(7).setCellValue(10);
            exempleRow1.createCell(8).setCellValue("3760012345678");
            exempleRow1.createCell(9).setCellValue("31/12/2024");
            exempleRow1.createCell(10).setCellValue("L12345");
            exempleRow1.createCell(11).setCellValue("Frais");
            exempleRow1.createCell(12).setCellValue(1.0);
            exempleRow1.createCell(13).setCellValue("L");
            exempleRow1.createCell(14).setCellValue("Oui");
            exempleRow1.createCell(15).setCellValue("France");

            for (int i = 0; i < enTetes.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] exporterProduitsVersExcel() throws IOException {
        List<Produit> produits = produitRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Produits");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] enTetes = {"ID", "Nom", "Catégorie", "Fournisseur", "Description",
                    "Prix Achat", "Prix Vente", "Quantité", "Seuil Alerte", "Code Barre",
                    "Date Création", "Date Péremption", "Lot", "Stockage", "Poids/Volume",
                    "Unité", "Bio", "Origine", "Marge", "Taux Marge", "Statut Stock",
                    "Statut Péremption"};

            for (int i = 0; i < enTetes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(enTetes[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Produit produit : produits) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(produit.getId());
                row.createCell(1).setCellValue(produit.getNom());
                row.createCell(2).setCellValue(produit.getCategorie().getNom());
                row.createCell(3).setCellValue(produit.getFournisseur() != null ? produit.getFournisseur().getNom() : "");
                row.createCell(4).setCellValue(produit.getDescription() != null ? produit.getDescription() : "");
                row.createCell(5).setCellValue(produit.getPrixAchat());
                row.createCell(6).setCellValue(produit.getPrixVente());
                row.createCell(7).setCellValue(produit.getQuantite());
                row.createCell(8).setCellValue(produit.getSeuilAlerte());
                row.createCell(9).setCellValue(produit.getCodeBarre() != null ? produit.getCodeBarre() : "");
                row.createCell(10).setCellValue(produit.getDateCreation().toString());
                row.createCell(11).setCellValue(produit.getDatePeremption() != null ? produit.getDatePeremption().toString() : "");
                row.createCell(12).setCellValue(produit.getLotNumber() != null ? produit.getLotNumber() : "");
                row.createCell(13).setCellValue(produit.getConditionsStockage() != null ? produit.getConditionsStockage() : "");
                row.createCell(14).setCellValue(produit.getPoidsVolume() != null ? produit.getPoidsVolume() : 0);
                row.createCell(15).setCellValue(produit.getUniteMesure() != null ? produit.getUniteMesure() : "");
                row.createCell(16).setCellValue(produit.isBio() ? "Oui" : "Non");
                row.createCell(17).setCellValue(produit.getOrigine() != null ? produit.getOrigine() : "");
                row.createCell(18).setCellValue(produit.getMarge());
                row.createCell(19).setCellValue(produit.getTauxMarge());
                row.createCell(20).setCellValue(produit.estStockFaible() ? "Stock Faible" : "Normal");

                String statutPeremption = "Normal";
                if (produit.estPerime()) {
                    statutPeremption = "Périmé";
                } else if (produit.estProchePeremption(7)) {
                    statutPeremption = "Proche péremption";
                }
                row.createCell(21).setCellValue(statutPeremption);
            }

            for (int i = 0; i < enTetes.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] exporterFournisseursVersExcel() throws IOException {
        List<Fournisseur> fournisseurs = fournisseurRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Fournisseurs");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] enTetes = {"ID", "Code", "Nom", "Téléphone", "Email", "Adresse",
                    "Contact", "Type Produits", "Délai Livraison", "Note", "Actif",
                    "Nombre Produits"};

            for (int i = 0; i < enTetes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(enTetes[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Fournisseur fournisseur : fournisseurs) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(fournisseur.getId());
                row.createCell(1).setCellValue(fournisseur.getCode());
                row.createCell(2).setCellValue(fournisseur.getNom());
                row.createCell(3).setCellValue(fournisseur.getTelephone() != null ? fournisseur.getTelephone() : "");
                row.createCell(4).setCellValue(fournisseur.getEmail() != null ? fournisseur.getEmail() : "");
                row.createCell(5).setCellValue(fournisseur.getAdresse() != null ? fournisseur.getAdresse() : "");

                String contact = "";
                if (fournisseur.getContactNom() != null) {
                    contact = fournisseur.getContactNom();
                    if (fournisseur.getContactTelephone() != null) {
                        contact += " (" + fournisseur.getContactTelephone() + ")";
                    }
                }
                row.createCell(6).setCellValue(contact);

                row.createCell(7).setCellValue(fournisseur.getTypeProduits() != null ? fournisseur.getTypeProduits() : "");
                row.createCell(8).setCellValue(fournisseur.getDelaiLivraison() != null ? fournisseur.getDelaiLivraison() : 0);
                row.createCell(9).setCellValue(fournisseur.getNote() != null ? fournisseur.getNote() : 0);
                row.createCell(10).setCellValue(fournisseur.isActif() ? "Oui" : "Non");
                row.createCell(11).setCellValue(fournisseur.getProduits().size());
            }

            for (int i = 0; i < enTetes.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}