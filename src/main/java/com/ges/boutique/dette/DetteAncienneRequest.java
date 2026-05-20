package com.ges.boutique.dette;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DetteAncienneRequest {
    private Long clientId;
    private Double montant;
    private LocalDate dateCredit;
    private String description;
}