package it.tivusat.cas.infrastructure.salesforce;

import it.tivusat.cas.domain.SmartcardType;
import org.springframework.stereotype.Component;

@Component
public class SalesforceSmartcardTypeMapper {

    private final SalesforceProperties properties;

    public SalesforceSmartcardTypeMapper(SalesforceProperties properties) {
        this.properties = properties;
    }

    public SmartcardType map(String salesforceType) {
        if (salesforceType == null || salesforceType.trim().isEmpty()) {
            throw new SalesforceException(
                    SalesforceException.FailureType.SMARTCARD_TYPE_MISSING,
                    "Salesforce smartcard type is missing",
                    null
            );
        }

        String normalizedType = salesforceType.trim();

        SmartcardType mappedType =
                properties.getTypeMapping().get(normalizedType);

        if (mappedType == null) {
            throw new SalesforceException(
                    SalesforceException.FailureType.UNSUPPORTED_SMARTCARD_TYPE,
                    "Salesforce smartcard type has no configured mapping",
                    null
            );
        }

        return mappedType;
    }
}