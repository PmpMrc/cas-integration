package it.tivusat.cas.infrastructure.salesforce;

public class SalesforceSmartcardData {

    private final String smartcardType;
    private final String crmVirtualState;

    public SalesforceSmartcardData(
            String smartcardType,
            String crmVirtualState
    ) {
        this.smartcardType = smartcardType;
        this.crmVirtualState = crmVirtualState;
    }

    public String getSmartcardType() {
        return smartcardType;
    }

    public String getCrmVirtualState() {
        return crmVirtualState;
    }
}