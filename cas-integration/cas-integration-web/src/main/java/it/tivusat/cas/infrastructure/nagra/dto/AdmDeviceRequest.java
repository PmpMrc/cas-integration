package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdmDeviceRequest {

    @JsonProperty("_id")
    private String id;

    private String status;
    private String caSN;
    private String smartCardId;
    private String accountUid;

    public AdmDeviceRequest() {
    }

    public AdmDeviceRequest(String id, String status, String caSN, String smartCardId, String accountUid) {
        this.id = id;
        this.status = status;
        this.caSN = caSN;
        this.smartCardId = smartCardId;
        this.accountUid = accountUid;
    }

    public static AdmDeviceRequest activate(String sn, String ua, String caSn) {
        return new AdmDeviceRequest(sn, "ENABLED", caSn, ua, sn);
    }

    public static AdmDeviceRequest refresh(String sn, String ua, String caSn) {
        return new AdmDeviceRequest(null, "ENABLED", caSn, ua, sn);
    }

    public static AdmDeviceRequest suspend() {
        return new AdmDeviceRequest(null, "DISABLED", null, null, null);
    }


    public String getId() {
        return id;
    }

    public String id() {
        return id;
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