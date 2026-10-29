package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardValidator;
import it.tivusat.cas.domain.UaRangeClassifier;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class PreloadSmartcardUseCase {

    private final SmartcardValidator smartcardValidator;
    private final NagraSmartcardGateway nagraSmartcardGateway;
    private final UaRangeClassifier uaRangeClassifier;

    public PreloadSmartcardUseCase(
            SmartcardValidator smartcardValidator,
            NagraSmartcardGateway nagraSmartcardGateway,
            UaRangeClassifier uaRangeClassifier
    ) {
        this.smartcardValidator = smartcardValidator;
        this.nagraSmartcardGateway = nagraSmartcardGateway;
        this.uaRangeClassifier = uaRangeClassifier;
    }

    public SmartcardResponse preload(PreloadSmartcardRequest request) {
        smartcardValidator.validate(request.sn());

        if (!"22".equals(request.productId())) {
            throw new IllegalArgumentException("productId must be 22");
        }

        if (request.smartcardType() == null) {
            throw new IllegalArgumentException("smartcardType is required");
        }

        if (request.source() == null) {
            throw new IllegalArgumentException("source is required");
        }

        String ua = smartcardValidator.extractUa(request.sn());
        uaRangeClassifier.validateRestSupported(ua);

        nagraSmartcardGateway.preloadSmartcard(
                request.sn(),
                ua,
                request.smartcardType(),
                request.source()
        );

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        return new SmartcardResponse(
                request.sn(),
                ua,
                request.smartcardType(),
                request.source(),
                SmartcardStatus.PRELOADED,
                true,
                true,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    static Instant expiryAfterFourYears(Instant validityFrom) {
        return validityFrom.atZone(ZoneOffset.UTC).plusYears(4).toInstant();
    }
}
