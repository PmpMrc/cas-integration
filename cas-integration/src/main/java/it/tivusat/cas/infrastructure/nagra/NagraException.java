package it.tivusat.cas.infrastructure.nagra;

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
}