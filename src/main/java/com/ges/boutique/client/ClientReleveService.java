package com.ges.boutique.client;

import com.ges.boutique.boutique.Boutique;
import com.ges.boutique.boutique.BoutiqueRepository;
import com.ges.boutique.vente.LigneVente;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientReleveService {

    private final VenteRepository venteRepository;
    private final ClientRepository clientRepository;
    private final BoutiqueRepository boutiqueRepository;

    private static final Color BLEU_PRIMAIRE = new Color(30, 80, 162);
    private static final Color BLEU_CLAIR = new Color(219, 234, 254);
    private static final Color ORANGE_CREDIT = new Color(234, 88, 12);
    private static final Color VERT_REGLE = new Color(22, 163, 74);
    private static final Color ROUGE_RETARD = new Color(220, 38, 38);
    private static final Color GRIS_CLAIR = new Color(248, 250, 252);
    private static final Color GRIS_TEXTE = new Color(71, 85, 105);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_LONG = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] genererReleve(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));

        List<Vente> ventes = venteRepository.findByClientId(clientId);
        if (ventes.isEmpty()) {
            String nomComplet = (client.getNom() + " " + client.getPrenom()).trim();
            ventes = venteRepository.findByClientNomOrTelephone(
                    nomComplet, client.getNumeroTelephone());
        }

        Boutique boutique = boutiqueRepository.findFirstByActifTrue()
                .orElse(new Boutique());

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 48, 48);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            ajouterEnTete(doc, writer, boutique, client);
            ajouterResume(doc, ventes, client);
            ajouterVentes(doc, ventes);
            ajouterCreditsEnCours(doc, ventes);
            ajouterPied(doc, boutique);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération relevé PDF client {}: {}", clientId, e.getMessage());
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    private void ajouterEnTete(Document doc, PdfWriter writer, Boutique boutique, Client client) throws DocumentException {
        // Bandeau bleu en-tête
        PdfContentByte canvas = writer.getDirectContentUnder();
        canvas.setColorFill(BLEU_PRIMAIRE);
        canvas.rectangle(36, doc.top() - 80, doc.right() - 36, 80);
        canvas.fill();

        // Nom boutique
        Font fontBoutiqueNom = new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE);
        Font fontBoutiqueAdresse = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(200, 220, 255));
        Font fontTitreReleve = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(180, 210, 255));

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.5f, 1f});

        PdfPCell cellGauche = new PdfPCell();
        cellGauche.setBorder(0);
        cellGauche.setBackgroundColor(BLEU_PRIMAIRE);
        cellGauche.setPadding(10);
        Paragraph nomBoutique = new Paragraph(boutique.getNom(), fontBoutiqueNom);
        cellGauche.addElement(nomBoutique);
        cellGauche.addElement(new Paragraph(boutique.getAdresse() + " — " + boutique.getTelephone(), fontBoutiqueAdresse));

        PdfPCell cellDroite = new PdfPCell();
        cellDroite.setBorder(0);
        cellDroite.setBackgroundColor(BLEU_PRIMAIRE);
        cellDroite.setPadding(10);
        cellDroite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellDroite.addElement(new Paragraph("RELEVÉ CLIENT", fontTitreReleve));
        Font fontDate = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.WHITE);
        cellDroite.addElement(new Paragraph("Édité le " + LocalDate.now().format(FMT), fontDate));

        header.addCell(cellGauche);
        header.addCell(cellDroite);
        doc.add(header);

        doc.add(new Paragraph(" "));

        // Infos client
        PdfPTable infoClient = new PdfPTable(2);
        infoClient.setWidthPercentage(100);
        infoClient.setWidths(new float[]{1f, 1f});
        infoClient.setSpacingBefore(8);

        Font fontLabel = new Font(Font.HELVETICA, 8, Font.BOLD, GRIS_TEXTE);
        Font fontValeur = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(15, 23, 42));
        Font fontValeurNormal = new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS_TEXTE);

        PdfPCell cellClient = new PdfPCell();
        cellClient.setBorderColor(BLEU_CLAIR);
        cellClient.setBorderWidth(1.5f);
        cellClient.setBackgroundColor(GRIS_CLAIR);
        cellClient.setPadding(10);
        String nomAffiche = (client.getNom() + " " + client.getPrenom()).trim();
        cellClient.addElement(new Paragraph("CLIENT", fontLabel));
        cellClient.addElement(new Paragraph(nomAffiche, fontValeur));
        if (client.getNumeroTelephone() != null)
            cellClient.addElement(new Paragraph("📱 " + client.getNumeroTelephone(), fontValeurNormal));
        if (client.getEmail() != null)
            cellClient.addElement(new Paragraph("✉ " + client.getEmail(), fontValeurNormal));
        if (client.getAdresse() != null)
            cellClient.addElement(new Paragraph("📍 " + client.getAdresse(), fontValeurNormal));

        PdfPCell cellDateClient = new PdfPCell();
        cellDateClient.setBorderColor(BLEU_CLAIR);
        cellDateClient.setBorderWidth(1.5f);
        cellDateClient.setBackgroundColor(GRIS_CLAIR);
        cellDateClient.setPadding(10);
        cellDateClient.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellDateClient.addElement(new Paragraph("MEMBRE DEPUIS", fontLabel));
        if (client.getDateCreation() != null)
            cellDateClient.addElement(new Paragraph(client.getDateCreation().toLocalDate().format(FMT), fontValeur));

        infoClient.addCell(cellClient);
        infoClient.addCell(cellDateClient);
        doc.add(infoClient);
        doc.add(new Paragraph(" "));
    }

    private void ajouterResume(Document doc, List<Vente> ventes, Client client) throws DocumentException {
        double totalAchats = ventes.stream().mapToDouble(v -> v.getMontantTotal() != null ? v.getMontantTotal() : 0).sum();
        long nbVentes = ventes.size();
        List<Vente> creditsEnCours = ventes.stream()
                .filter(v -> Boolean.TRUE.equals(v.getEstCredit()) && !Boolean.TRUE.equals(v.getCreditRegle()))
                .collect(Collectors.toList());
        double totalCreditsRestants = creditsEnCours.stream()
                .mapToDouble(v -> v.getMontantRestant() != null ? v.getMontantRestant() : 0).sum();

        Font fontTitre = new Font(Font.HELVETICA, 11, Font.BOLD, BLEU_PRIMAIRE);
        doc.add(new Paragraph("RÉSUMÉ DU COMPTE", fontTitre));
        doc.add(new Paragraph(" "));

        PdfPTable resume = new PdfPTable(3);
        resume.setWidthPercentage(100);
        resume.setSpacingBefore(4);

        addResumeCard(resume, "Total achats", formatMontant(totalAchats), BLEU_PRIMAIRE, Color.WHITE);
        addResumeCard(resume, "Nb. ventes", String.valueOf(nbVentes), new Color(15, 118, 110), Color.WHITE);
        addResumeCard(resume, "Crédits en cours", formatMontant(totalCreditsRestants),
                creditsEnCours.isEmpty() ? new Color(71, 85, 105) : ORANGE_CREDIT, Color.WHITE);

        doc.add(resume);
        doc.add(new Paragraph(" "));
    }

    private void addResumeCard(PdfPTable table, String label, String valeur, Color bg, Color textColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setPadding(12);
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Font fontLabel = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(200, 220, 255));
        Font fontValeur = new Font(Font.HELVETICA, 13, Font.BOLD, textColor);
        cell.addElement(new Paragraph(label, fontLabel));
        Paragraph p = new Paragraph(valeur, fontValeur);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        table.addCell(cell);
    }

    private void ajouterVentes(Document doc, List<Vente> ventes) throws DocumentException {
        Font fontTitre = new Font(Font.HELVETICA, 11, Font.BOLD, BLEU_PRIMAIRE);
        doc.add(new Paragraph("HISTORIQUE DES VENTES", fontTitre));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.4f, 1.2f, 1f, 2.5f, 1.2f, 1f});

        String[] headers = {"N° Vente", "Date", "Type", "Produits", "Montant", "Statut"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(BLEU_PRIMAIRE);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Font fontCell = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(30, 30, 30));
        Font fontCellBold = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(30, 30, 30));
        boolean pair = false;

        for (Vente v : ventes) {
            Color bg = pair ? Color.WHITE : GRIS_CLAIR;
            pair = !pair;

            addCell(table, v.getNumeroVente() != null ? v.getNumeroVente() : "#" + v.getId(), fontCellBold, bg, Element.ALIGN_LEFT);
            addCell(table, v.getDateVente() != null ? v.getDateVente().format(FMT) : "-", fontCell, bg, Element.ALIGN_CENTER);

            String type = Boolean.TRUE.equals(v.getEstCredit()) ? "Crédit" : "Comptant";
            Color typeColor = Boolean.TRUE.equals(v.getEstCredit()) ? ORANGE_CREDIT : VERT_REGLE;
            PdfPCell typeCell = new PdfPCell(new Phrase(type, new Font(Font.HELVETICA, 8, Font.BOLD, typeColor)));
            typeCell.setBackgroundColor(bg);
            typeCell.setPadding(5);
            typeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(typeCell);

            String produits = v.getLignes() != null
                    ? v.getLignes().stream()
                    .map(l -> l.getProduit() != null ? l.getProduit().getNom() + " ×" + l.getQuantite() : "")
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(", "))
                    : "-";
            if (produits.length() > 60) produits = produits.substring(0, 57) + "...";
            addCell(table, produits, fontCell, bg, Element.ALIGN_LEFT);

            addCell(table, formatMontant(v.getMontantTotal() != null ? v.getMontantTotal() : 0), fontCellBold, bg, Element.ALIGN_RIGHT);

            String statut;
            Color statutColor;
            if (Boolean.TRUE.equals(v.getAnnulee())) {
                statut = "Annulée"; statutColor = GRIS_TEXTE;
            } else if (!Boolean.TRUE.equals(v.getEstCredit())) {
                statut = "Payé"; statutColor = VERT_REGLE;
            } else if (Boolean.TRUE.equals(v.getCreditRegle())) {
                statut = "Réglé"; statutColor = VERT_REGLE;
            } else if (v.getDateEcheance() != null && v.getDateEcheance().isBefore(LocalDate.now())) {
                statut = "Retard"; statutColor = ROUGE_RETARD;
            } else {
                statut = "En cours"; statutColor = ORANGE_CREDIT;
            }

            PdfPCell statutCell = new PdfPCell(new Phrase(statut, new Font(Font.HELVETICA, 8, Font.BOLD, statutColor)));
            statutCell.setBackgroundColor(bg);
            statutCell.setPadding(5);
            statutCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(statutCell);
        }

        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void ajouterCreditsEnCours(Document doc, List<Vente> ventes) throws DocumentException {
        List<Vente> credits = ventes.stream()
                .filter(v -> Boolean.TRUE.equals(v.getEstCredit()) && !Boolean.TRUE.equals(v.getCreditRegle()))
                .collect(Collectors.toList());

        if (credits.isEmpty()) return;

        Font fontTitre = new Font(Font.HELVETICA, 11, Font.BOLD, ORANGE_CREDIT);
        doc.add(new Paragraph("CRÉDITS EN COURS", fontTitre));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.4f, 1.2f, 1.2f, 1.2f, 1.2f});

        String[] headers = {"N° Vente", "Date vente", "Montant total", "Déjà versé", "Reste à payer"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(ORANGE_CREDIT);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Font fontCell = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(30, 30, 30));
        Font fontReste = new Font(Font.HELVETICA, 9, Font.BOLD, ROUGE_RETARD);
        boolean pair = false;

        for (Vente v : credits) {
            Color bg = pair ? Color.WHITE : new Color(255, 247, 237);
            pair = !pair;
            double reste = v.getMontantRestant() != null ? v.getMontantRestant() :
                    (v.getMontantTotal() != null ? v.getMontantTotal() : 0) - (v.getMontantVerse() != null ? v.getMontantVerse() : 0);

            addCell(table, v.getNumeroVente() != null ? v.getNumeroVente() : "#" + v.getId(), fontCell, bg, Element.ALIGN_LEFT);
            addCell(table, v.getDateVente() != null ? v.getDateVente().format(FMT) : "-", fontCell, bg, Element.ALIGN_CENTER);
            addCell(table, formatMontant(v.getMontantTotal() != null ? v.getMontantTotal() : 0), fontCell, bg, Element.ALIGN_RIGHT);
            addCell(table, formatMontant(v.getMontantVerse() != null ? v.getMontantVerse() : 0), fontCell, bg, Element.ALIGN_RIGHT);

            PdfPCell resteCell = new PdfPCell(new Phrase(formatMontant(reste), fontReste));
            resteCell.setBackgroundColor(bg);
            resteCell.setPadding(5);
            resteCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(resteCell);
        }

        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void ajouterPied(Document doc, Boutique boutique) throws DocumentException {
        Font fontPied = new Font(Font.HELVETICA, 8, Font.ITALIC, GRIS_TEXTE);
        Paragraph pied = new Paragraph("Document généré par " + boutique.getNom() +
                " — " + boutique.getTelephone() + " — " + LocalDate.now().format(FMT), fontPied);
        pied.setAlignment(Element.ALIGN_CENTER);
        doc.add(new LineSeparator());
        doc.add(pied);
    }

    private void addCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private String formatMontant(double montant) {
        return NumberFormat.getNumberInstance(Locale.FRANCE).format((long) montant) + " F";
    }
}
