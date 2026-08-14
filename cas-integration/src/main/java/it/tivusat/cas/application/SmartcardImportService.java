package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ImportSmartcardRowResult;
import it.tivusat.cas.api.dto.ImportSmartcardsResponse;
import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.exception.UnsupportedSmartcardRangeException;
import it.tivusat.cas.infrastructure.nagra.NagraException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class SmartcardImportService {

    private final PreloadSmartcardUseCase preloadSmartcardUseCase;

    public SmartcardImportService(PreloadSmartcardUseCase preloadSmartcardUseCase) {
        this.preloadSmartcardUseCase = preloadSmartcardUseCase;
    }

    public ImportSmartcardsResponse importSmartcards(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Import file is mandatory and cannot be empty");
        }

        List<ImportSmartcardRowResult> results = new ArrayList<>();

        int totalRows = 0;
        int successRows = 0;
        boolean headerAlreadyChecked = false;

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
                )
        ) {
            String line;
            int physicalRowNumber = 0;

            while ((line = reader.readLine()) != null) {
                physicalRowNumber++;

                String normalizedLine = removeBom(line).trim();

                if (normalizedLine.isBlank() || normalizedLine.startsWith("#")) {
                    continue;
                }

                if (!headerAlreadyChecked) {
                    headerAlreadyChecked = true;

                    if (isHeader(normalizedLine)) {
                        continue;
                    }
                }

                totalRows++;

                try {
                    PreloadSmartcardRequest request = parseLine(normalizedLine);

                    preloadSmartcardUseCase.preload(request);

                    successRows++;

                    results.add(
                            ImportSmartcardRowResult.success(
                                    physicalRowNumber,
                                    request.sn()
                            )
                    );

                } catch (Exception ex) {
                    results.add(
                            ImportSmartcardRowResult.error(
                                    physicalRowNumber,
                                    extractSnSafely(normalizedLine),
                                    classifyError(ex),
                                    ex.getMessage()
                            )
                    );
                }
            }

        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read import file", ex);
        }

        return new ImportSmartcardsResponse(
                totalRows,
                successRows,
                totalRows - successRows,
                results
        );
    }

    private PreloadSmartcardRequest parseLine(String line) {
        String[] columns = splitLine(line);

        if (columns.length != 4) {
            throw new IllegalArgumentException(
                    "Expected 4 columns: sn, smartcardType, source, productId"
            );
        }

        String sn = clean(columns[0]);
        SmartcardType smartcardType = parseSmartcardType(clean(columns[1]));
        SmartcardSource source = parseSource(clean(columns[2]));
        String productId = clean(columns[3]);

        if (productId.isBlank()) {
            throw new IllegalArgumentException("productId is mandatory");
        }

        return new PreloadSmartcardRequest(
                sn,
                smartcardType,
                source,
                productId
        );
    }

    private String[] splitLine(String line) {
        if (line.contains(";")) {
            return line.split(";", -1);
        }

        return line.split(",", -1);
    }

    private SmartcardType parseSmartcardType(String value) {
        try {
            return SmartcardType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid smartcardType: " + value);
        }
    }

    private SmartcardSource parseSource(String value) {
        try {
            return SmartcardSource.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid source: " + value);
        }
    }

    private boolean isHeader(String line) {
        String lowerCaseLine = line.toLowerCase();

        return lowerCaseLine.contains("sn")
                && lowerCaseLine.contains("smartcardtype")
                && lowerCaseLine.contains("source")
                && lowerCaseLine.contains("productid");
    }

    private String clean(String value) {
        String cleaned = value.trim();

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned.trim();
    }

    private String removeBom(String value) {
        if (value.startsWith("\uFEFF")) {
            return value.substring(1);
        }

        return value;
    }

    private String extractSnSafely(String line) {
        String[] columns = splitLine(line);

        if (columns.length == 0) {
            return null;
        }

        String sn = clean(columns[0]);

        return sn.isBlank() ? null : sn;
    }

    private String classifyError(Exception ex) {
        if (ex instanceof UnsupportedSmartcardRangeException) {
            return "UNSUPPORTED_SMARTCARD_RANGE";
        }

        if (ex instanceof NagraException) {
            return "NAGRA_ERROR";
        }

        if (ex instanceof IllegalArgumentException) {
            return "INVALID_ROW";
        }

        return "INTERNAL_ERROR";
    }
}