package it.tivusat.cas.api.dto;

public class ImportSmartcardRowResult {

    private int rowNumber;
    private String sn;
    private boolean success;
    private String error;
    private String message;

    public ImportSmartcardRowResult() {
    }

    public ImportSmartcardRowResult(int rowNumber, String sn, boolean success, String error, String message) {
        this.rowNumber = rowNumber;
        this.sn = sn;
        this.success = success;
        this.error = error;
        this.message = message;
    }

    public static ImportSmartcardRowResult success(int rowNumber, String sn, String message) {
        return new ImportSmartcardRowResult(rowNumber, sn, true, null, message);
    }

    public static ImportSmartcardRowResult error(int rowNumber, String sn, String error, String message) {
        return new ImportSmartcardRowResult(rowNumber, sn, false, error, message);
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getSn() {
        return sn;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public int rowNumber() {
        return rowNumber;
    }

    public String sn() {
        return sn;
    }

    public boolean success() {
        return success;
    }

    public String error() {
        return error;
    }

    public String message() {
        return message;
    }
}