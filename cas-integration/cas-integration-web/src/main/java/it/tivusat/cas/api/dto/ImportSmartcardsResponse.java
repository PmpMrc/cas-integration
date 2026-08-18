package it.tivusat.cas.api.dto;

import java.util.List;

public class ImportSmartcardsResponse {

    private int totalRows;
    private int successRows;
    private int failedRows;
    private List<ImportSmartcardRowResult> results;

    public ImportSmartcardsResponse() {
    }

    public ImportSmartcardsResponse(int totalRows, int successRows, int failedRows, List<ImportSmartcardRowResult> results) {
        this.totalRows = totalRows;
        this.successRows = successRows;
        this.failedRows = failedRows;
        this.results = results;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getSuccessRows() {
        return successRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public List<ImportSmartcardRowResult> getResults() {
        return results;
    }

    public int totalRows() {
        return totalRows;
    }

    public int successRows() {
        return successRows;
    }

    public int failedRows() {
        return failedRows;
    }

    public List<ImportSmartcardRowResult> results() {
        return results;
    }
}