package it.tivusat.cas.api.dto;

public class ImportSmartcardRowResult {

    private int lineNumber;
    private String originalValue;
    private String sn;
    private String ua;
    private boolean success;
    private String status;
    private String message;

    public ImportSmartcardRowResult() {
    }

    public ImportSmartcardRowResult(
            int lineNumber,
            String originalValue,
            String sn,
            String ua,
            boolean success,
            String status,
            String message
    ) {
        this.lineNumber = lineNumber;
        this.originalValue = originalValue;
        this.sn = sn;
        this.ua = ua;
        this.success = success;
        this.status = status;
        this.message = message;
    }

    public static ImportSmartcardRowResult success(
            int lineNumber,
            String originalValue,
            String sn,
            String ua,
            String status
    ) {
        return new ImportSmartcardRowResult(
                lineNumber,
                originalValue,
                sn,
                ua,
                true,
                status,
                null
        );
    }

    public static ImportSmartcardRowResult failure(
            int lineNumber,
            String originalValue,
            String message
    ) {
        return new ImportSmartcardRowResult(
                lineNumber,
                originalValue,
                null,
                null,
                false,
                "ERROR",
                message
        );
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public String getSn() {
        return sn;
    }

    public String getUa() {
        return ua;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}