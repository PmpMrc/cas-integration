package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardStatus;

public final class NagraSmartcardStatusMapper {

    private NagraSmartcardStatusMapper() {
    }

    public static SmartcardStatus resolve(
            String accountStatus,
            String deviceStatus,
            String endpoint
    ) {
        if ("SUSPENDED".equalsIgnoreCase(accountStatus)) {
            return SmartcardStatus.DISABLED;
        }

        if ("CANCELLED".equalsIgnoreCase(accountStatus)) {
            return SmartcardStatus.DELETED;
        }

        if ("ACTIVE".equalsIgnoreCase(accountStatus)) {
            if ("DISABLED".equalsIgnoreCase(deviceStatus)) {
                return SmartcardStatus.PRELOADED;
            }

            if ("ENABLED".equalsIgnoreCase(deviceStatus)) {
                return SmartcardStatus.ENABLED;
            }
        }

        throw new NagraException(
                null,
                "Cannot determine smartcard status from NAGRA response: "
                        + "accountStatus=" + accountStatus
                        + ", deviceStatus=" + deviceStatus,
                null,
                NagraOperation.ADM_GET_BUNDLED_ACCOUNT,
                endpoint,
                NagraException.FailureType.INVALID_RESPONSE
        );
    }
}