package it.tivusat.cas.domain.exception;

public class SmartcardNotFoundException extends RuntimeException {

    public SmartcardNotFoundException(String sn) {
        super("Smartcard not found: " + sn);
    }
}