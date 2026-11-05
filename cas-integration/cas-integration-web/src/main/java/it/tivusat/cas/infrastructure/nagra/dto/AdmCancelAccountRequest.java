package it.tivusat.cas.infrastructure.nagra.dto;

public final class AdmCancelAccountRequest {

    private final String status = "CANCELLED";
    private final String cancelICC = "true";

    public String getStatus() {
        return status;
    }

    public String getCancelICC() {
        return cancelICC;
    }
}