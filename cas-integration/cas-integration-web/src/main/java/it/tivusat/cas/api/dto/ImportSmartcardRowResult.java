package it.tivusat.cas.api.dto;

public record ImportSmartcardRowResult(
        int rowNumber,
        String sn,
        boolean success,
        String error,
        String message
) {

    public static ImportSmartcardRowResult success(int rowNumber, String sn) {
        return new ImportSmartcardRowResult(
                rowNumber,
                sn,
                true,
                null,
                "Smartcard imported successfully"
        );
    }

    public static ImportSmartcardRowResult error(
            int rowNumber,
            String sn,
            String error,
            String message
    ) {
        return new ImportSmartcardRowResult(
                rowNumber,
                sn,
                false,
                error,
                message
        );
    }
}