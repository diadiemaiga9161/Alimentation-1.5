package com.ges.boutique.resultat;

import com.ges.boutique.bonus.BonusFournisseurRepository;
import com.ges.boutique.depense.DepenseRepository;
import com.ges.boutique.employe.PaiementEmployeRepository;
import com.ges.boutique.feature.CleFonctionnalite;
import com.ges.boutique.feature.RequireFeature;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resultat-net")
@RequiredArgsConstructor
@RequireFeature(CleFonctionnalite.RESULTAT_NET)
public class ResultatNetController {

    private final VenteRepository venteRepository;
    private final DepenseRepository depenseRepository;
    private final BonusFournisseurRepository bonusRepository;
    private final PaiementEmployeRepository paiementEmployeRepository;

    @GetMapping("/journalier")
    public ResponseEntity<Map<String, Object>> journalier(
            @RequestParam(required = false) String date) {
        LocalDate jour = date != null ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime debut = jour.atStartOfDay();
        LocalDateTime fin = jour.atTime(LocalTime.MAX);

        double benefice = safe(venteRepository.getBeneficeTotalByDateRange(debut, fin));
        double bonus = safe(bonusRepository.sumMontantByPeriode(jour, jour));
        double depenses = safe(depenseRepository.sumMontantByPeriode(jour, jour))
                        + safe(paiementEmployeRepository.sumMontantByPeriode(debut, fin));
        double resultat = benefice + bonus - depenses;

        return ResponseEntity.ok(build("JOURNALIER", jour.toString(), jour.toString(),
                benefice, bonus, depenses, resultat));
    }

    @GetMapping("/mensuel")
    public ResponseEntity<Map<String, Object>> mensuel(
            @RequestParam(defaultValue = "0") int mois,
            @RequestParam(defaultValue = "0") int annee) {
        int m = mois > 0 ? mois : LocalDate.now().getMonthValue();
        int a = annee > 0 ? annee : LocalDate.now().getYear();

        LocalDate premier = LocalDate.of(a, m, 1);
        LocalDate dernier = premier.withDayOfMonth(premier.lengthOfMonth());

        double benefice = safe(venteRepository.getBeneficeTotalByDateRange(
                premier.atStartOfDay(), dernier.atTime(LocalTime.MAX)));
        double bonus = safe(bonusRepository.sumMontantByMoisAnnee(m, a));
        double depenses = safe(depenseRepository.sumMontantByMoisAnnee(m, a))
                        + safe(paiementEmployeRepository.sumMontantByMoisAnnee(m, a));
        double resultat = benefice + bonus - depenses;

        return ResponseEntity.ok(build("MENSUEL", premier.toString(), dernier.toString(),
                benefice, bonus, depenses, resultat));
    }

    @GetMapping("/annuel")
    public ResponseEntity<Map<String, Object>> annuel(
            @RequestParam(defaultValue = "0") int annee) {
        int a = annee > 0 ? annee : LocalDate.now().getYear();

        LocalDate premier = LocalDate.of(a, 1, 1);
        LocalDate dernier = LocalDate.of(a, 12, 31);

        double benefice = safe(venteRepository.getBeneficeTotalByDateRange(
                premier.atStartOfDay(), dernier.atTime(LocalTime.MAX)));
        double bonus = safe(bonusRepository.sumMontantByAnnee(a));
        double depenses = safe(depenseRepository.sumMontantByAnnee(a))
                        + safe(paiementEmployeRepository.sumMontantByAnnee(a));
        double resultat = benefice + bonus - depenses;

        return ResponseEntity.ok(build("ANNUEL", premier.toString(), dernier.toString(),
                benefice, bonus, depenses, resultat));
    }

    @GetMapping("/periode")
    public ResponseEntity<Map<String, Object>> parPeriode(
            @RequestParam String debut,
            @RequestParam String fin) {
        LocalDate d = LocalDate.parse(debut);
        LocalDate f = LocalDate.parse(fin);

        double benefice = safe(venteRepository.getBeneficeTotalByDateRange(
                d.atStartOfDay(), f.atTime(LocalTime.MAX)));
        double bonus = safe(bonusRepository.sumMontantByPeriode(d, f));
        double depenses = safe(depenseRepository.sumMontantByPeriode(d, f))
                        + safe(paiementEmployeRepository.sumMontantByPeriode(d.atStartOfDay(), f.atTime(LocalTime.MAX)));
        double resultat = benefice + bonus - depenses;

        return ResponseEntity.ok(build("PERIODE", debut, fin, benefice, bonus, depenses, resultat));
    }

    private Map<String, Object> build(String periode, String dateDebut, String dateFin,
                                       double benefice, double bonus, double depenses, double resultat) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("periode", periode);
        r.put("dateDebut", dateDebut);
        r.put("dateFin", dateFin);
        r.put("benefices", arrondi(benefice));
        r.put("bonusFournisseurs", arrondi(bonus));
        r.put("depenses", arrondi(depenses));
        r.put("resultatNet", arrondi(resultat));
        r.put("etat", resultat >= 0 ? "GAIN" : "PERTE");
        return r;
    }

    private double safe(Double v) { return v != null ? v : 0.0; }

    private double arrondi(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
