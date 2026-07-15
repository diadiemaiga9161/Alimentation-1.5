package com.ges.boutique.rapport;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/previsions")
@RequiredArgsConstructor
public class PrevisionStockController {

    private final PrevisionStockService previsionStockService;

    @GetMapping("/stock")
    public ResponseEntity<List<PrevisionStockDTO>> previsionStock() {
        return ResponseEntity.ok(previsionStockService.calculerPrevisions());
    }
}
