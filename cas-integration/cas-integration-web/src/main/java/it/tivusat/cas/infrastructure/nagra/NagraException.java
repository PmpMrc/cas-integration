package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;

import java.util.Locale;

public class NagraException extends RuntimeException {

    public enum FailureType {
        HTTP_ERROR,
        NETWORK_ERROR,
        INVALID_RESPONSE
    }

    private final Integer httpStatus;
    private final String responseBody;
    private final NagraOperation operation;
    private final String endpoint;
    private final FailureType failureType;
    private final String requestPayload;

    public NagraException(Integer httpStatus, String responseBody) {
        this(httpStatus, responseBody, null, null, null,
                httpStatus == null ? FailureType.NETWORK_ERROR : FailureType.HTTP_ERROR);
    }

    public NagraException(Integer httpStatus, String responseBody, Throwable cause) {
        this(httpStatus, responseBody, cause, null, null,
                httpStatus == null ? FailureType.NETWORK_ERROR : FailureType.HTTP_ERROR);
    }

    public NagraException(
            Integer httpStatus,
            String responseBody,
            Throwable cause,
            NagraOperation operation,
            String endpoint,
            FailureType failureType
    ) {
        this(httpStatus, responseBody, cause, operation, endpoint, failureType, null);
    }

    public NagraException(
            Integer httpStatus,
            String responseBody,
            Throwable cause,
            NagraOperation operation,
            String endpoint,
            FailureType failureType,
            String requestPayload
    ) {
        super("NAGRA call failed with HTTP status " + httpStatus + ": " + responseBody, cause);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.operation = operation;
        this.endpoint = endpoint;
        this.failureType = failureType;
        this.requestPayload = requestPayload;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public NagraOperation getOperation() {
        return operation;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public boolean isAlreadyExists() {
        if (httpStatus == null || httpStatus != 409 || responseBody == null) {
            return false;
        }

        String message = responseBody.toLowerCase(Locale.ROOT);
        return message.contains("already exist") || message.contains("duplicate");
    }

    public boolean isDeviceNotFound() {
        if (httpStatus == null || httpStatus != 404 || responseBody == null) {
            return false;
        }

        return responseBody.contains("\"errorCode\":4403")
                || responseBody.contains("\"errorCode\":\"4403\"")
                || responseBody.contains("Device not found");
    }
}
