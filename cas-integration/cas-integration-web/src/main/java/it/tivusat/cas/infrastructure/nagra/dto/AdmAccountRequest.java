package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class AdmAccountRequest {

    @JsonProperty("_id")
    private String id;

    private String status;
    private List<Object> billingAddress;
    private String suspensionMode;

    public AdmAccountRequest() {
    }

    public AdmAccountRequest(String id, String status, List<Object> billingAddress, String suspensionMode) {
        this.id = id;
        this.status = status;
        this.billingAddress = billingAddress;
        this.suspensionMode = suspensionMode;
    }

    public static AdmAccountRequest active(String sn) {
        return new AdmAccountRequest(sn, "ACTIVE", Collections.emptyList(), "MOP");
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

    public List<Object> getBillingAddress() {
        return billingAddress;
    }

    public List<Object> billingAddress() {
        return billingAddress;
    }

    public String getSuspensionMode() {
        return suspensionMode;
    }

    public String suspensionMode() {
        return suspensionMode;
    }
}