package com.ges.boutique.employe;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
public class EmployeController {

    private final EmployeService employeService;

    @PostMapping
    public ResponseEntity<EmployeDto> creer(@RequestBody EmployeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeService.creerEmploye(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeDto> modifier(@PathVariable Long id, @RequestBody EmployeRequest request) {
        return ResponseEntity.ok(employeService.modifierEmploye(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeService.getEmployeById(id));
    }

    @GetMapping
    public ResponseEntity<List<EmployeDto>> getTous() {
        return ResponseEntity.ok(employeService.getTousLesEmployes());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<EmployeDto>> getActifs() {
        return ResponseEntity.ok(employeService.getEmployesActifs());
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        employeService.desactiverEmploye(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<Void> activer(@PathVariable Long id) {
        employeService.activerEmploye(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        employeService.supprimerEmploye(id);
        return ResponseEntity.noContent().build();
    }
}
