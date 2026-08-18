package it.tivusat.cas.api.dto;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;

import javax.validation.constraints.NotNull;

public class ActivateSmartcardRequest {

    @NotNull
    private SmartcardSource source;

    @NotNull
    private SmartcardType smartcardType;

    private String caSn;

    public ActivateSmartcardRequest() {
    }

    public ActivateSmartcardRequest(SmartcardSource source, SmartcardType smartcardType, String caSn) {
        this.source = source;
        this.smartcardType = smartcardType;
        this.caSn = caSn;
    }

    public SmartcardSource getSource() {
        return source;
    }

    public void setSource(SmartcardSource source) {
        this.source = source;
    }

    public SmartcardSource source() {
        return source;
    }

    public SmartcardType getSmartcardType() {
        return smartcardType;
    }

    public void setSmartcardType(SmartcardType smartcardType) {
        this.smartcardType = smartcardType;
    }

    public SmartcardType smartcardType() {
        return smartcardType;
    }

    public String getCaSn() {
        return caSn;
    }

    public void setCaSn(String caSn) {
        this.caSn = caSn;
    }

    public String caSn() {
        return caSn;
    }
}