package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.domain.SmartcardValidator;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;
import it.tivusat.cas.infrastructure.persistence.SmartcardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PreloadSmartcardUseCase {

    private final SmartcardRepository repository;
    private final SmartcardValidator validator;

    public PreloadSmartcardUseCase(
            SmartcardRepository repository,
            SmartcardValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public void preload(PreloadSmartcardRequest request) {
        validator.validateSerialNumber(request.sn());

        String ua = validator.extractUa(request.sn());

        SmartcardEntity smartcard = repository.findById(request.sn())
                .orElseGet(() -> new SmartcardEntity(
                        request.sn(),
                        ua,
                        request.smartcardType(),
                        request.source()
                ));

        smartcard.markPreloaded(
                request.smartcardType().getNagraType(),
                request.productId(),
                Instant.now().plusSeconds(60L * 60 * 24 * 365 * 4)
        );

        repository.save(smartcard);
    }
}