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
        String ua = validateSnAndExtractUa(sn);

        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (smartcardType == null) {
            throw new IllegalArgumentException("smartcardType is required");
        }
        if (source != SmartcardSource.PHYSICAL) {
            throw new IllegalArgumentException(
                    "Virtual smartcard cancellation is not available yet; "
                            + "the NAGRA cancellation policy must be confirmed"
            );
        }

        nagraSmartcardGateway.deleteSmartcard(
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
                SmartcardStatus.DELETED,
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

    public SmartcardResponse getStatus(String sn, SmartcardSource source) {
        String ua = validateSnAndExtractUa(sn);

        NagraSmartcardStatusSnapshot snapshot =
                nagraSmartcardGateway.fetchSmartcardStatus(sn, source);

        if (snapshot.skipped()) {
            return baseResponse(sn, ua, null, source, SmartcardStatus.ERROR, false, null);
        }

        NagraEntitlementResponse entitlement = snapshot.entitlement();

        if (snapshot.deviceNotFound()) {
            throw new SmartcardNotFoundException(sn);
        }

        NagraDeviceResponse device = snapshot.device();
        String deviceStatus = device == null ? null : device.status();
        SmartcardStatus status;
        if ("DISABLED".equalsIgnoreCase(deviceStatus)) {
            status = SmartcardStatus.DISABLED;
        } else if ("ENABLED".equalsIgnoreCase(deviceStatus)) {
            status = SmartcardStatus.ENABLED;
        } else {
            throw new NagraException(
                    null,
                    "ADM device returned an unexpected status for smartcard " + sn
                            + ": " + deviceStatus,
                    null,
                    NagraOperation.ADM_GET_DEVICE,
                    null,
                    NagraException.FailureType.INVALID_RESPONSE
            );
        }

        return new SmartcardResponse(
                sn,
                ua,
                null,
                source,
                status,
                true,
                true,
                entitlement != null ? entitlement.id() : null,
                entitlement != null ? entitlement.productId() : null,
                device.caSN(),
                entitlement != null ? entitlement.expiryDate() : null,
                Instant.now(),
                Instant.now()
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