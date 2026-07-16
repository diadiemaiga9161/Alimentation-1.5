package com.ges.boutique.parametres;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametresRequest {
    private boolean soldeCaisse;
    private boolean historiqueOperationsCaisse;
    private boolean creditsRegles;
    private boolean historiqueVentesAnnulees;
}
