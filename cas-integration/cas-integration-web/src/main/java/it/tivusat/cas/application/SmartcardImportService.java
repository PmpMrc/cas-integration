package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ImportSmartcardRowResult;
import it.tivusat.cas.api.dto.ImportSmartcardsResponse;
import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.SmartcardValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SmartcardImportService {

    private final PreloadSmartcardUseCase preloadSmartcardUseCase;
    private final SmartcardValidator smartcardValidator;

    public SmartcardImportService(
            PreloadSmartcardUseCase preloadSmartcardUseCase,
            SmartcardValidator smartcardValidator
    ) {
        this.preloadSmartcardUseCase = preloadSmartcardUseCase;
        this.smartcardValidator = smartcardValidator;
    }

    public ImportSmartcardsResponse importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Import file is required");
        }

        List<ImportSmartcardRowResult> results = new ArrayList<ImportSmartcardRowResult>();
        int totalRows = 0;
        int successCount = 0;
        int failureCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String headerLine = reader.readLine();

            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("Import file header is required");
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);

            validateRequiredHeaders(headerIndex);

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                totalRows++;

                try {
                    List<String> columns = parseCsvLine(line);

                    String snOrUa = readSnOrUa(columns, headerIndex);
                    SmartcardSource source = readSource(columns, headerIndex);
                    SmartcardType smartcardType = readSmartcardType(columns, headerIndex);
                    String productId = readRequiredString(columns, headerIndex, "productid", "productId is required");

                    String sn = normalizeToSn(snOrUa);
                    String ua = smartcardValidator.extractUa(sn);

                    PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                            sn,
                            smartcardType,
                            source,
                            productId
                    );

                    SmartcardResponse response = preloadSmartcardUseCase.preload(request);

                    successCount++;

                    results.add(ImportSmartcardRowResult.success(
                            lineNumber,
                            snOrUa,
                            response.getSn(),
                            response.getUa(),
                            response.getStatus().name()
                    ));
                } catch (Exception exception) {
                    failureCount++;

                    results.add(ImportSmartcardRowResult.failure(
                            lineNumber,
                            line,
                            exception.getMessage()
                    ));
                }
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to read import file: " + exception.getMessage(), exception);
        }

        return new ImportSmartcardsResponse(
                file.getOriginalFilename(),
                totalRows,
                successCount,
                failureCount,
                results
        );
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        boolean hasSnOrUa = headerIndex.containsKey("sn") || headerIndex.containsKey("ua");

        if (!hasSnOrUa) {
            throw new IllegalArgumentException("Import file must contain either 'sn' or 'ua' column");
        }

        if (!headerIndex.containsKey("source")) {
            throw new IllegalArgumentException("Import file must contain 'source' column");
        }

        if (!headerIndex.containsKey("smartcardtype")) {
            throw new IllegalArgumentException("Import file must contain 'smartcardType' column");
        }

        if (!headerIndex.containsKey("productid")) {
            throw new IllegalArgumentException("Import file must contain 'productId' column");
        }
    }

    private String readSnOrUa(List<String> columns, Map<String, Integer> headerIndex) {
        if (headerIndex.containsKey("sn")) {
            return readRequiredString(columns, headerIndex, "sn", "sn is required");
        }

        return readRequiredString(columns, headerIndex, "ua", "ua is required");
    }

    private SmartcardSource readSource(List<String> columns, Map<String, Integer> headerIndex) {
        String value = readRequiredString(columns, headerIndex, "source", "source is required");

        try {
            return SmartcardSource.valueOf(value.trim().toUpperCase());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid source: " + value);
        }
    }

    private SmartcardType readSmartcardType(List<String> columns, Map<String, Integer> headerIndex) {
        String value = readRequiredString(columns, headerIndex, "smartcardtype", "smartcardType is required");

        try {
            return SmartcardType.valueOf(value.trim().toUpperCase());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid smartcardType: " + value);
        }
    }

    private String readRequiredString(
            List<String> columns,
            Map<String, Integer> headerIndex,
            String headerName,
            String errorMessage
    ) {
        Integer index = headerIndex.get(headerName);

        if (index == null || index >= columns.size()) {
            throw new IllegalArgumentException(errorMessage);
        }

        String value = cleanValue(columns.get(index));

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value;
    }

    private String normalizeToSn(String value) {
        String cleanedValue = cleanValue(value);

        if (cleanedValue.matches("\\d{10}")) {
            return smartcardValidator.buildSnFromUa(cleanedValue);
        }

        if (cleanedValue.matches("\\d{12}")) {
            smartcardValidator.validateSn(cleanedValue);
            return cleanedValue;
        }

        throw new IllegalArgumentException("Row must contain UA 10 digits or SN 12 digits");
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<String, Integer>();

        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = normalizeHeader(headers.get(i));

            if (normalizedHeader != null && !normalizedHeader.isEmpty()) {
                headerIndex.put(normalizedHeader, i);
            }
        }

        return headerIndex;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return null;
        }

        String normalized = header.trim()
                .replace("\"", "")
                .replace("'", "")
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase();

        if ("smartcardtype".equals(normalized) || "type".equals(normalized)) {
            return "smartcardtype";
        }

        if ("productid".equals(normalized) || "product".equals(normalized)) {
            return "productid";
        }

        return normalized;
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }

        return value.trim()
                .replace("\"", "")
                .replace("'", "");
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();

        if (line == null) {
            return values;
        }

        char separator = detectSeparator(line);

        StringBuilder currentValue = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);

            if (currentChar == '"') {
                insideQuotes = !insideQuotes;
            } else if (currentChar == separator && !insideQuotes) {
                values.add(cleanValue(currentValue.toString()));
                currentValue.setLength(0);
            } else {
                currentValue.append(currentChar);
            }
        }

        values.add(cleanValue(currentValue.toString()));

        return values;
    }

    private char detectSeparator(String line) {
        if (line.indexOf(';') >= 0) {
            return ';';
        }

        if (line.indexOf('\t') >= 0) {
            return '\t';
        }

        if (line.indexOf('|') >= 0) {
            return '|';
        }

        return ',';
    }
}