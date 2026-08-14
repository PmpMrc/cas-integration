package it.tivusat.cas.api;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.application.PreloadSmartcardUseCase;
import it.tivusat.cas.application.SmartcardOperationsUseCase;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;
import it.tivusat.cas.api.dto.ImportSmartcardsResponse;
import it.tivusat.cas.application.SmartcardImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/smartcards")
public class SmartcardController {

    private final PreloadSmartcardUseCase preloadSmartcardUseCase;
    private final SmartcardOperationsUseCase smartcardOperationsUseCase;
    private final SmartcardImportService smartcardImportService;

    public SmartcardController(
            PreloadSmartcardUseCase preloadSmartcardUseCase,
            SmartcardOperationsUseCase smartcardOperationsUseCase,
            SmartcardImportService smartcardImportService
    ) {
        this.preloadSmartcardUseCase = preloadSmartcardUseCase;
        this.smartcardOperationsUseCase = smartcardOperationsUseCase;
        this.smartcardImportService = smartcardImportService;
    }

    @PostMapping("/preload")
    public ResponseEntity<Void> preload(@Valid @RequestBody PreloadSmartcardRequest request) {
        preloadSmartcardUseCase.preload(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{sn}/activate")
    public ResponseEntity<SmartcardResponse> activate(
            @PathVariable String sn,
            @RequestBody(required = false) ActivateSmartcardRequest request
    ) {
        ActivateSmartcardRequest safeRequest = request != null
                ? request
                : new ActivateSmartcardRequest(null);

        SmartcardEntity smartcard = smartcardOperationsUseCase.activate(sn, safeRequest);
        return ResponseEntity.ok(SmartcardResponse.fromEntity(smartcard));
    }

    @PostMapping("/{sn}/refresh")
    public ResponseEntity<SmartcardResponse> refresh(
            @PathVariable String sn,
            @RequestBody(required = false) ActivateSmartcardRequest request
    ) {
        ActivateSmartcardRequest safeRequest = request != null
                ? request
                : new ActivateSmartcardRequest(null);

        SmartcardEntity smartcard = smartcardOperationsUseCase.refresh(sn, safeRequest);
        return ResponseEntity.ok(SmartcardResponse.fromEntity(smartcard));
    }

    @PostMapping("/{sn}/suspend")
    public ResponseEntity<SmartcardResponse> suspend(@PathVariable String sn) {
        SmartcardEntity smartcard = smartcardOperationsUseCase.suspend(sn);
        return ResponseEntity.ok(SmartcardResponse.fromEntity(smartcard));
    }

    @DeleteMapping("/{sn}")
    public ResponseEntity<SmartcardResponse> delete(@PathVariable String sn) {
        SmartcardEntity smartcard = smartcardOperationsUseCase.delete(sn);
        return ResponseEntity.ok(SmartcardResponse.fromEntity(smartcard));
    }

    @GetMapping("/{sn}/status")
    public ResponseEntity<SmartcardResponse> getStatus(@PathVariable String sn) {
        SmartcardEntity smartcard = smartcardOperationsUseCase.getStatus(sn);
        return ResponseEntity.ok(SmartcardResponse.fromEntity(smartcard));
    }

    @PostMapping("/{sn}/sync-status")
    public ResponseEntity<SmartcardResponse> syncStatus(@PathVariable String sn) {
        SmartcardEntity smartcard = smartcardOperationsUseCase.syncStatus(sn);
        return ResponseEntity.ok(SmartcardResponse.fromEntity(smartcard));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportSmartcardsResponse> importSmartcards(@RequestPart("file") MultipartFile file) {
        ImportSmartcardsResponse response = smartcardImportService.importSmartcards(file);
        return ResponseEntity.accepted().body(response);
    }
}