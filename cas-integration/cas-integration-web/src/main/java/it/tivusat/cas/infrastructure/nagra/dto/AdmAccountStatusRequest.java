package it.tivusat.cas.infrastructure.nagra.dto;

public final class AdmAccountStatusRequest {

    private final String status;

    private AdmAccountStatusRequest(String status) {
        this.status = status;
    }

    public static AdmAccountStatusRequest active() {
        return new AdmAccountStatusRequest("ACTIVE");
    }

    public String getStatus() {
        return status;
    }
}