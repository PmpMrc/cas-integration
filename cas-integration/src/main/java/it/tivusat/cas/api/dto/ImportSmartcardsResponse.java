package it.tivusat.cas.api.dto;

import java.util.List;

public record ImportSmartcardsResponse(
        int totalRows,
        int successRows,
        int failedRows,
        List<ImportSmartcardRowResult> results
) {
}