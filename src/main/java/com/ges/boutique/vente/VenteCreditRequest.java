package com.ges.boutique.vente;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class VenteCreditRequest extends VenteRequest {

    // Constructeur qui force estCredit = true
    public VenteCreditRequest() {
        super();
        this.setEstCredit(true);
    }
}