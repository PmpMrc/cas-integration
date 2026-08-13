package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.domain.SmartcardValidator;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardGateway;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;
import it.tivusat.cas.infrastructure.persistence.SmartcardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PreloadSmartcardUseCase {

    private final SmartcardRepository repository;
    private final SmartcardValidator validator;
    private final NagraSmartcardGateway nagraSmartcardGateway;

    public PreloadSmartcardUseCase(
            SmartcardRepository repository,
            SmartcardValidator validator,
            NagraSmartcardGateway nagraSmartcardGateway
    ) {
        this.repository = repository;
        this.validator = validator;
        this.nagraSmartcardGateway = nagraSmartcardGateway;
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

        Instant validityFrom = Instant.now();
        Instant expiryDate = validityFrom.plus(365L * 4, ChronoUnit.DAYS);

        nagraSmartcardGateway.preloadSmartcard(
                request.sn(),
                request.smartcardType(),
                request.source(),
                request.productId(),
                validityFrom,
                expiryDate
        );

        smartcard.markPreloaded(
                request.smartcardType().getNagraType(),
                request.productId(),
                expiryDate
        );

        repository.save(smartcard);
    }
}