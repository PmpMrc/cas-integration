package it.tivusat.cas.api.dto;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;

import javax.validation.constraints.NotNull;

public class ReactivateSmartcardRequest {

    @NotNull
    private SmartcardSource source;

    @NotNull
    private SmartcardType smartcardType;

    public ReactivateSmartcardRequest() {
    }

    public ReactivateSmartcardRequest(
            SmartcardSource source,
            SmartcardType smartcardType
    ) {
        this.source = source;
        this.smartcardType = smartcardType;
    }

    public SmartcardSource getSource() {
        return source;
    }

    public void setSource(SmartcardSource source) {
        this.source = source;
    }

    public SmartcardType getSmartcardType() {
        return smartcardType;
    }

    public void setSmartcardType(SmartcardType smartcardType) {
        this.smartcardType = smartcardType;
    }
}