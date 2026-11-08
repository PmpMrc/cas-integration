package it.tivusat.cas.infrastructure.salesforce;

public class SalesforceException extends RuntimeException {

    public enum FailureType {
        INTEGRATION_DISABLED,
        AUTHENTICATION_ERROR,
        NETWORK_ERROR,
        INVALID_RESPONSE,
        QUERY_ERROR,
        SMARTCARD_NOT_FOUND,
        AMBIGUOUS_SMARTCARD,
        SMARTCARD_TYPE_MISSING,
        UNSUPPORTED_SMARTCARD_TYPE
    }

    private final FailureType failureType;
    private final Integer upstreamHttpStatus;

    public SalesforceException(
            FailureType failureType,
            String message,
            Integer upstreamHttpStatus
    ) {
        super(message);
        this.failureType = failureType;
        this.upstreamHttpStatus = upstreamHttpStatus;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public Integer getUpstreamHttpStatus() {
        return upstreamHttpStatus;
    }
}