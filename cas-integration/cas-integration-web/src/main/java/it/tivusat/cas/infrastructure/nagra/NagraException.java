package it.tivusat.cas.infrastructure.nagra;

import java.util.Locale;

public class NagraException extends RuntimeException {

    private final Integer httpStatus;
    private final String responseBody;

    public NagraException(Integer httpStatus, String responseBody) {
        super("NAGRA call failed with HTTP status " + httpStatus + ": " + responseBody);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public NagraException(Integer httpStatus, String responseBody, Throwable cause) {
        super("NAGRA call failed with HTTP status " + httpStatus + ": " + responseBody, cause);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
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
