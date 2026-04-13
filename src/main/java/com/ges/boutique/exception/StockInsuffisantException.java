package com.ges.boutique.exception;

import lombok.Getter;

@Getter
public class StockInsuffisantException extends RuntimeException {
    private final Long produitId;
    private final Integer stockDisponible;
    private final Integer stockRequis;

    public StockInsuffisantException(String message) {
        super(message);
        this.produitId = null;
        this.stockDisponible = null;
        this.stockRequis = null;
    }

    public StockInsuffisantException(String message, Long produitId,
                                     Integer stockDisponible, Integer stockRequis) {
        super(message);
        this.produitId = produitId;
        this.stockDisponible = stockDisponible;
        this.stockRequis = stockRequis;
    }
}