package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
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

    public SmartcardResponse activate(String sn, ActivateSmartcardRequest request) {
        String ua = validateSnAndExtractUa(sn);
        validateCaSnIfRequired(request.smartcardType(), request.caSn());

        nagraSmartcardGateway.activateSmartcard(sn, ua, request.source(), request.caSn());

        return baseResponse(
                sn,
                ua,
                request.smartcardType(),
                request.source(),
                SmartcardStatus.ENABLED,
                true,
                request.caSn()
        );
    }

    public SmartcardResponse refresh(String sn, ActivateSmartcardRequest request) {
        String ua = validateSnAndExtractUa(sn);
        validateCaSnIfRequired(request.smartcardType(), request.caSn());

        nagraSmartcardGateway.refreshSmartcard(sn, ua, request.source(), request.caSn());

        return baseResponse(
                sn,
                ua,
                request.smartcardType(),
                request.source(),
                SmartcardStatus.ENABLED,
                true,
                request.caSn()
        );
    }

    public SmartcardResponse suspend(String sn, SmartcardSource source) {
        String ua = validateSnAndExtractUa(sn);

        nagraSmartcardGateway.suspendSmartcard(sn, ua, source);

        return baseResponse(
                sn,
                ua,
                null,
                source,
                SmartcardStatus.DISABLED,
                true,
                null
        );
    }

    public SmartcardResponse delete(String sn, SmartcardSource source) {
        String ua = validateSnAndExtractUa(sn);

        nagraSmartcardGateway.deleteSmartcard(sn, source);

        return baseResponse(
                sn,
                ua,
                null,
                source,
                SmartcardStatus.DELETED,
                false,
                null
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
            if (entitlement == null) {
                throw new SmartcardNotFoundException(sn);
            }
            return new SmartcardResponse(
                    sn,
                    ua,
                    null,
                    source,
                    SmartcardStatus.PRELOADED,
                    true,
                    false,
                    entitlement != null ? entitlement.id() : null,
                    entitlement != null ? entitlement.productId() : null,
                    null,
                    entitlement != null ? entitlement.expiryDate() : null,
                    Instant.now(),
                    Instant.now()
            );
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