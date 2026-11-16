package it.tivusat.cas.application;

import it.tivusat.cas.domain.SmartcardType;

public class ResolvedSmartcardData {

    private final SmartcardType smartcardType;
    private final String crmVirtualState;

    public ResolvedSmartcardData(
            SmartcardType smartcardType,
            String crmVirtualState
    ) {
        this.smartcardType = smartcardType;
        this.crmVirtualState = crmVirtualState;
    }

    public SmartcardType getSmartcardType() {
        return smartcardType;
    }

    public String getCrmVirtualState() {
        return crmVirtualState;
    }
}