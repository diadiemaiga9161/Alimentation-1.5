package com.ges.boutique.vente;

public class VenteException extends RuntimeException {
    public VenteException(String message) {
        super(message);
    }

    public VenteException(String message, Throwable cause) {
        super(message, cause);
    }
}
