package com.ges.boutique.mobilemoney;

import com.ges.boutique.vente.ModePaiement;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mobile-money")
@RequiredArgsConstructor
@Tag(name = "Mobile Money", description = "Statistiques Orange Money et Moov Money")
public class MobileMoneyController {

    private final VenteRepository venteRepository;

    private static final List<ModePaiement> MOBILE_MONEY_MODES =
            List.of(ModePaiement.ORANGE_MONEY, ModePaiement.MOOV_MONEY);

    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Liste des opérations mobile money par période")
    public ResponseEntity<Map<String, Object>> getOperations(
            @RequestParam(defaultValue = "TOUS") String type,
            @RequestParam(defaultValue = "JOUR") String periode) {

        LocalDateTime[] range = getPeriodeRange(periode);
        LocalDateTime debut = range[0];
        LocalDateTime fin = range[1];

        List<Vente> ventes;
        if ("ORANGE_MONEY".equals(type)) {
            ventes = venteRepository.findByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, debut, fin);
        } else if ("MOOV_MONEY".equals(type)) {
            ventes = venteRepository.findByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, debut, fin);
        } else {
            ventes = venteRepository.findByModePaiementInAndDateRange(MOBILE_MONEY_MODES, debut, fin);
        }

        double totalOrange = ventes.stream()
                .filter(v -> ModePaiement.ORANGE_MONEY == v.getModePaiement())
                .mapToDouble(v -> v.getMontantTotal() != null ? v.getMontantTotal() : 0)
                .sum();
        double totalMoov = ventes.stream()
                .filter(v -> ModePaiement.MOOV_MONEY == v.getModePaiement())
                .mapToDouble(v -> v.getMontantTotal() != null ? v.getMontantTotal() : 0)
                .sum();

        List<Map<String, Object>> operations = ventes.stream().map(v -> {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("id", v.getId());
            op.put("numeroVente", v.getNumeroVente());
            op.put("dateVente", v.getDateVente());
            op.put("montantTotal", v.getMontantTotal());
            op.put("modePaiement", v.getModePaiement());
            op.put("referencePaiement", v.getReferencePaiement());
            op.put("clientNom", v.getClientNom());
            op.put("vendeurNom", v.getVendeur() != null ? v.getVendeur().getNomComplet() : "");
            return op;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operations", operations);
        result.put("totalOrangeMoney", totalOrange);
        result.put("totalMoovMoney", totalMoov);
        result.put("totalGlobal", totalOrange + totalMoov);
        result.put("nombreOperations", ventes.size());
        result.put("periode", periode);
        result.put("type", type);
        result.put("debut", debut);
        result.put("fin", fin);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Résumé mobile money (tous les modes et périodes)")
    public ResponseEntity<Map<String, Object>> getResume() {
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime finJour = LocalDate.now().atTime(LocalTime.MAX);

        LocalDateTime debutSemaine = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime finSemaine = LocalDate.now().with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);

        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMois = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX);

        LocalDateTime debutAnnee = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime finAnnee = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear()).atTime(LocalTime.MAX);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("jour", buildResumePeriode(debutJour, finJour));
        result.put("semaine", buildResumePeriode(debutSemaine, finSemaine));
        result.put("mois", buildResumePeriode(debutMois, finMois));
        result.put("annee", buildResumePeriode(debutAnnee, finAnnee));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Export CSV des opérations mobile money")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(defaultValue = "TOUS") String type,
            @RequestParam(defaultValue = "MOIS") String periode) {

        LocalDateTime[] range = getPeriodeRange(periode);
        List<Vente> ventes;
        if ("ORANGE_MONEY".equals(type)) {
            ventes = venteRepository.findByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, range[0], range[1]);
        } else if ("MOOV_MONEY".equals(type)) {
            ventes = venteRepository.findByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, range[0], range[1]);
        } else {
            ventes = venteRepository.findByModePaiementInAndDateRange(MOBILE_MONEY_MODES, range[0], range[1]);
        }

        StringBuilder csv = new StringBuilder();
        csv.append('﻿'); // BOM UTF-8
        csv.append("N° Vente;Date;Mode Paiement;Montant (F CFA);Référence;Client;Vendeur\n");
        for (Vente v : ventes) {
            csv.append(esc(v.getNumeroVente())).append(';');
            csv.append(v.getDateVente() != null ? v.getDateVente().toString() : "").append(';');
            csv.append(v.getModePaiement() != null ? v.getModePaiement().name() : "").append(';');
            csv.append(v.getMontantTotal() != null ? v.getMontantTotal().longValue() : 0).append(';');
            csv.append(esc(v.getReferencePaiement())).append(';');
            csv.append(esc(v.getClientNom())).append(';');
            csv.append(v.getVendeur() != null ? esc(v.getVendeur().getNomComplet()) : "").append('\n');
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"mobile-money-" + periode.toLowerCase() + ".csv\"")
                .body(bytes);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace(";", ",");
    }

    private Map<String, Object> buildResumePeriode(LocalDateTime debut, LocalDateTime fin) {
        double orange = venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, debut, fin);
        double moov = venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, debut, fin);
        long nbOrange = venteRepository.countByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, debut, fin);
        long nbMoov = venteRepository.countByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, debut, fin);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("orangeMoney", orange);
        r.put("moovMoney", moov);
        r.put("total", orange + moov);
        r.put("nombreOrange", nbOrange);
        r.put("nombreMoov", nbMoov);
        r.put("nombreTotal", nbOrange + nbMoov);
        return r;
    }

    private LocalDateTime[] getPeriodeRange(String periode) {
        return switch (periode.toUpperCase()) {
            case "SEMAINE" -> new LocalDateTime[]{
                    LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(),
                    LocalDate.now().with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX)
            };
            case "MOIS" -> new LocalDateTime[]{
                    LocalDate.now().withDayOfMonth(1).atStartOfDay(),
                    LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX)
            };
            case "ANNEE" -> new LocalDateTime[]{
                    LocalDate.now().withDayOfYear(1).atStartOfDay(),
                    LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear()).atTime(LocalTime.MAX)
            };
            default -> new LocalDateTime[]{
                    LocalDate.now().atStartOfDay(),
                    LocalDate.now().atTime(LocalTime.MAX)
            };
        };
    }
}
