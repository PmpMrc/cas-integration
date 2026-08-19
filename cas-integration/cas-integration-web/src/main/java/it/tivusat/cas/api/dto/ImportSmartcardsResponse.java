package it.tivusat.cas.api.dto;

import java.util.List;

public class ImportSmartcardsResponse {

    private String fileName;
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<ImportSmartcardRowResult> results;

    public ImportSmartcardsResponse() {
    }

    public ImportSmartcardsResponse(
            String fileName,
            int totalRows,
            int successCount,
            int failureCount,
            List<ImportSmartcardRowResult> results
    ) {
        this.fileName = fileName;
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = results;
    }

    public String getFileName() {
        return fileName;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public List<ImportSmartcardRowResult> getResults() {
        return results;
    }
}