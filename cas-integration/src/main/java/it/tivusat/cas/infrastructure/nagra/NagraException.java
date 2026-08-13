package it.tivusat.cas.infrastructure.nagra;

public class NagraException extends RuntimeException {

    private final int httpStatus;
    private final String responseBody;

    public NagraException(int httpStatus, String responseBody) {
        super("NAGRA call failed with HTTP status " + httpStatus + ": " + responseBody);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }
}