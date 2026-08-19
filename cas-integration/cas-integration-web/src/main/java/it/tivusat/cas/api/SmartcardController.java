package it.tivusat.cas.api;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.application.PreloadSmartcardUseCase;
import it.tivusat.cas.application.SmartcardOperationsUseCase;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import it.tivusat.cas.api.dto.ImportSmartcardsResponse;
import it.tivusat.cas.application.SmartcardImportService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

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
    public ResponseEntity<SmartcardResponse> preload(
            @Valid @RequestBody PreloadSmartcardRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(preloadSmartcardUseCase.preload(request));
    }

    @PostMapping("/{sn}/activate")
    public ResponseEntity<SmartcardResponse> activate(
            @PathVariable("sn") String sn,
            @Valid @RequestBody ActivateSmartcardRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(smartcardOperationsUseCase.activate(sn, request));
    }

    @PostMapping("/{sn}/refresh")
    public ResponseEntity<SmartcardResponse> refresh(
            @PathVariable("sn") String sn,
            @Valid @RequestBody ActivateSmartcardRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(smartcardOperationsUseCase.refresh(sn, request));
    }

    @PostMapping("/{sn}/suspend")
    public ResponseEntity<SmartcardResponse> suspend(
            @PathVariable("sn") String sn,
            @RequestParam("source") SmartcardSource source
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(smartcardOperationsUseCase.suspend(sn, source));
    }

    @DeleteMapping("/{sn}")
    public ResponseEntity<SmartcardResponse> delete(
            @PathVariable("sn") String sn,
            @RequestParam("source") SmartcardSource source
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(smartcardOperationsUseCase.delete(sn, source));
    }

    @GetMapping("/{sn}/status")
    public ResponseEntity<SmartcardResponse> status(
            @PathVariable("sn") String sn,
            @RequestParam("source") SmartcardSource source
    ) {
        return ResponseEntity.ok(smartcardOperationsUseCase.getStatus(sn, source));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportSmartcardsResponse> importSmartcards(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                smartcardImportService.importFile(file)
        );
    }

}