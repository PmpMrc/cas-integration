package it.tivusat.cas.application;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.salesforce.SalesforceSmartcardClient;
import it.tivusat.cas.infrastructure.salesforce.SalesforceSmartcardData;
import it.tivusat.cas.infrastructure.salesforce.SalesforceSmartcardTypeMapper;
import org.springframework.stereotype.Component;

@Component
public class SmartcardTypeResolver {

    private final SalesforceSmartcardClient salesforceClient;
    private final SalesforceSmartcardTypeMapper typeMapper;

    public SmartcardTypeResolver(
            SalesforceSmartcardClient salesforceClient,
            SalesforceSmartcardTypeMapper typeMapper
    ) {
        this.salesforceClient = salesforceClient;
        this.typeMapper = typeMapper;
    }

    public SmartcardType resolve(
            String sn,
            SmartcardType requestedType
    ) {
        if (requestedType != null) {
            return requestedType;
        }

        String salesforceType = salesforceClient.findSmartcardType(sn);

        return typeMapper.map(salesforceType);
    }

    public ResolvedSmartcardData resolveForStatus(
            String sn,
            SmartcardSource source
    ) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }

        SalesforceSmartcardData data = salesforceClient.findSmartcard(sn);

        SmartcardType resolvedType =
                source == SmartcardSource.VIRTUAL
                        ? SmartcardType.TIVU_VIRTUAL
                        : typeMapper.map(data.getSmartcardType());

        return new ResolvedSmartcardData(
                resolvedType,
                data.getCrmVirtualState()
        );
    }
}