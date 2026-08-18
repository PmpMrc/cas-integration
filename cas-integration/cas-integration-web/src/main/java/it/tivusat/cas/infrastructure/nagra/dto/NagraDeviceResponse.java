package it.tivusat.cas.infrastructure.nagra.dto;

public class NagraDeviceResponse {

    private String status;
    private String caSN;
    private String smartCardId;
    private String accountUid;

    public NagraDeviceResponse() {
    }

    public NagraDeviceResponse(String status, String caSN, String smartCardId, String accountUid) {
        this.status = status;
        this.caSN = caSN;
        this.smartCardId = smartCardId;
        this.accountUid = accountUid;
    }

    public String getStatus() {
        return status;
    }

    public String status() {
        return status;
    }

    public String getCaSN() {
        return caSN;
    }

    public String caSN() {
        return caSN;
    }

    public String getSmartCardId() {
        return smartCardId;
    }

    public String smartCardId() {
        return smartCardId;
    }

    public String getAccountUid() {
        return accountUid;
    }

    public String accountUid() {
        return accountUid;
    }
}