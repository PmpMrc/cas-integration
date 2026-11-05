package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.api.dto.ReactivateSmartcardRequest;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.exception.SmartcardNotFoundException;
import it.tivusat.cas.infrastructure.nagra.NagraException;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.SmartcardValidator;
import it.tivusat.cas.domain.UaRangeClassifier;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardGateway;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardStatusSnapshot;
import it.tivusat.cas.infrastructure.nagra.dto.NagraDeviceResponse;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import it.tivusat.cas.infrastructure.nagra.SubscriptionEntitlementFactory;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import it.tivusat.cas.infrastructure.nagra.dto.NagraBundledDeviceResponse;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SmartcardOperationsUseCase {

    private final SmartcardValidator smartcardValidator;
    private final UaRangeClassifier uaRangeClassifier;
    private final NagraSmartcardGateway nagraSmartcardGateway;

    public SmartcardOperationsUseCase(
            SmartcardValidator smartcardValidator,
            UaRangeClassifier uaRangeClassifier,
            NagraSmartcardGateway nagraSmartcardGateway
    ) {
        this.smartcardValidator = smartcardValidator;
        this.uaRangeClassifier = uaRangeClassifier;
        this.nagraSmartcardGateway = nagraSmartcardGateway;
    }

    public SmartcardResponse activate(
            String sn,
            ActivateSmartcardRequest request
    ) {
        String ua = validateSnAndExtractUa(sn);

        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.source() == null) {
            throw new IllegalArgumentException("source is required");
        }

        validateCaSnIfRequired(request.smartcardType(), request.caSn());

        RmgEntitlementRequest entitlement =
                SubscriptionEntitlementFactory.create(
                        sn,
                        request.smartcardType(),
                        Instant.now()
                );

        nagraSmartcardGateway.activateSmartcard(
                sn,
                request.source(),
                entitlement
        );

        return new SmartcardResponse(
                sn,
                ua,
                request.smartcardType(),
                request.source(),
                SmartcardStatus.ENABLED,
                true,
                true,
                entitlement.id(),
                entitlement.productId(),
                null,
                entitlement.expiryDate(),
                entitlement.validFrom(),
                entitlement.validFrom()
        );
    }

    public SmartcardResponse refresh(
            String sn,
            ActivateSmartcardRequest request
    ) {
        String ua = validateSnAndExtractUa(sn);

        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.source() == null) {
            throw new IllegalArgumentException("source is required");
        }

        SmartcardType smartcardType =
                resolveRefreshSmartcardType(sn, request.smartcardType());

        nagraSmartcardGateway.refreshSmartcard(
                sn,
                smartcardType,
                request.source()
        );

        Instant now = Instant.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        return new SmartcardResponse(
                sn,
                ua,
                smartcardType,
                request.source(),
                SmartcardStatus.ENABLED,
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

    public SmartcardResponse suspend(
            String sn,
            SmartcardSource source,
            SmartcardType smartcardType
    ) {
        String ua = validateSnAndExtractUa(sn);

        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (smartcardType == null) {
            throw new IllegalArgumentException("smartcardType is required");
        }

        nagraSmartcardGateway.suspendSmartcard(
                sn,
                smartcardType,
                source
        );

        Instant now = Instant.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        return new SmartcardResponse(
                sn,
                ua,
                smartcardType,
                source,
                SmartcardStatus.DISABLED,
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

    public SmartcardResponse delete(
            String sn,
            SmartcardSource source,
            SmartcardType smartcardType
    ) {
        return delete(sn, source, smartcardType, false);
    }

    public SmartcardResponse delete(
            String sn,
            SmartcardSource source,
            SmartcardType smartcardType,
            boolean purgeForReuse
    ) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (smartcardType == null) {
            throw new IllegalArgumentException("smartcardType is required");
        }
        if (purgeForReuse && source != SmartcardSource.VIRTUAL) {
            throw new IllegalArgumentException(
                    "purgeForReuse is allowed only for virtual smartcards"
            );
        }

        String ua = validateSnAndExtractUa(sn);

        nagraSmartcardGateway.deleteSmartcard(
                sn,
                smartcardType,
                source,
                purgeForReuse
        );

        Instant now = Instant.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        return new SmartcardResponse(
                sn,
                ua,
                smartcardType,
                source,
                SmartcardStatus.DELETED,
                !purgeForReuse,
                !purgeForReuse,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public SmartcardResponse reactivate(
            String sn,
            ReactivateSmartcardRequest request
    ) {
        String ua = validateSnAndExtractUa(sn);

        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getSource() == null) {
            throw new IllegalArgumentException("source is required");
        }

        RmgEntitlementRequest entitlement =
                SubscriptionEntitlementFactory.create(
                        sn,
                        request.getSmartcardType(),
                        Instant.now()
                );

        nagraSmartcardGateway.reactivateSmartcard(
                sn,
                request.getSource(),
                entitlement
        );

        return new SmartcardResponse(
                sn,
                ua,
                request.getSmartcardType(),
                request.getSource(),
                SmartcardStatus.ENABLED,
                true,
                true,
                entitlement.id(),
                entitlement.productId(),
                null,
                entitlement.expiryDate(),
                entitlement.validFrom(),
                entitlement.validFrom()
        );
    }

    public SmartcardResponse getStatus(
            String sn,
            SmartcardSource source,
            SmartcardType smartcardType
    ) {
        String ua = validateSnAndExtractUa(sn);

        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (smartcardType == null) {
            throw new IllegalArgumentException(
                    "smartcardType is required for status; "
                            + "Salesforce lookup is not available yet"
            );
        }

        NagraSmartcardStatusSnapshot snapshot =
                nagraSmartcardGateway.fetchSmartcardStatus(
                        sn,
                        smartcardType,
                        source
                );

        NagraBundledDeviceResponse device = snapshot.bundledDevice();
        NagraEntitlementResponse entitlement = snapshot.entitlement();

        return new SmartcardResponse(
                sn,
                ua,
                smartcardType,
                source,
                snapshot.status(),
                true,
                device != null,
                entitlement != null ? entitlement.id() : null,
                entitlement != null ? entitlement.productId() : null,
                device != null ? device.getCaSn() : null,
                entitlement != null ? entitlement.expiryDate() : null,
                snapshot.account().getCreatedAt(),
                snapshot.account().getUpdatedAt()
        );
    }

    private String validateSnAndExtractUa(String sn) {
        smartcardValidator.validate(sn);
        String ua = smartcardValidator.extractUa(sn);
        uaRangeClassifier.validateRestSupported(ua);
        return ua;
    }

    private void validateCaSnIfRequired(SmartcardType smartcardType, String caSn) {
        if (SmartcardType.TIVU_HD_PAIRING.equals(smartcardType)
                && (caSn == null || caSn.trim().isEmpty())) {
            throw new IllegalArgumentException("caSn is required for TIVU_HD_PAIRING smartcards");
        }
    }

    private SmartcardType resolveRefreshSmartcardType(
            String sn,
            SmartcardType requestedType
    ) {
        if (requestedType == null) {
            throw new IllegalArgumentException(
                    "smartcardType is required for refresh of smartcard "
                            + sn
                            + "; Salesforce lookup is not available yet"
            );
        }

        return requestedType;
    }

    private SmartcardResponse baseResponse(
            String sn,
            String ua,
            SmartcardType smartcardType,
            SmartcardSource source,
            SmartcardStatus status,
            boolean deviceCreated,
            String caSn
    ) {
        Instant now = Instant.now();

        return new SmartcardResponse(
                sn,
                ua,
                smartcardType,
                source,
                status,
                true,
                deviceCreated,
                smartcardType != null ? sn + "_" + smartcardType.getNagraType() : null,
                null,
                caSn,
                null,
                now,
                now
        );
    }
}