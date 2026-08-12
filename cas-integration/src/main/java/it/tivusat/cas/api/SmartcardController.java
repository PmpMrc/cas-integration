package it.tivusat.cas.api;

import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.application.PreloadSmartcardUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/smartcards")
public class SmartcardController {

    private final PreloadSmartcardUseCase preloadSmartcardUseCase;

    public SmartcardController(PreloadSmartcardUseCase preloadSmartcardUseCase) {
        this.preloadSmartcardUseCase = preloadSmartcardUseCase;
    }

    @PostMapping("/preload")
    public ResponseEntity<Void> preload(@Valid @RequestBody PreloadSmartcardRequest request) {
        preloadSmartcardUseCase.preload(request);
        return ResponseEntity.accepted().build();
    }
}