package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NagraSmartcardStatusMapperTest {

    private static final String ENDPOINT =
            "/adm/v1/bundled/accounts/109687603246_TivuHD";

    @Test
    void activeAccountAndDisabledDeviceShouldBePreloaded() {
        assertEquals(
                SmartcardStatus.PRELOADED,
                resolve("ACTIVE", "DISABLED")
        );
    }

    @Test
    void activeAccountAndEnabledDeviceShouldBeEnabled() {
        assertEquals(
                SmartcardStatus.ENABLED,
                resolve("ACTIVE", "ENABLED")
        );
    }

    @Test
    void suspendedAccountShouldOverrideEnabledDevice() {
        assertEquals(
                SmartcardStatus.DISABLED,
                resolve("SUSPENDED", "ENABLED")
        );
    }

    @Test
    void suspendedAccountShouldNotRequireDeviceStatus() {
        assertEquals(
                SmartcardStatus.DISABLED,
                resolve("SUSPENDED", null)
        );
    }

    @Test
    void cancelledAccountShouldOverrideEnabledDevice() {
        assertEquals(
                SmartcardStatus.DELETED,
                resolve("CANCELLED", "ENABLED")
        );
    }

    @Test
    void cancelledAccountShouldNotRequireDeviceStatus() {
        assertEquals(
                SmartcardStatus.DELETED,
                resolve("CANCELLED", null)
        );
    }

    @Test
    void activeAccountWithoutDeviceStatusShouldFail() {
        NagraException error = assertThrows(
                NagraException.class,
                () -> resolve("ACTIVE", null)
        );

        assertEquals(
                NagraException.FailureType.INVALID_RESPONSE,
                error.getFailureType()
        );
        assertEquals(
                NagraOperation.ADM_GET_BUNDLED_ACCOUNT,
                error.getOperation()
        );
        assertEquals(ENDPOINT, error.getEndpoint());
    }

    @Test
    void unknownDeviceStatusOnActiveAccountShouldFail() {
        assertThrows(
                NagraException.class,
                () -> resolve("ACTIVE", "PENDING")
        );
    }

    @Test
    void unknownAccountStatusShouldNotBecomeEnabled() {
        assertThrows(
                NagraException.class,
                () -> resolve("UNKNOWN", "ENABLED")
        );
    }

    @Test
    void missingAccountStatusShouldNotBecomeEnabled() {
        assertThrows(
                NagraException.class,
                () -> resolve(null, "ENABLED")
        );
    }

    private SmartcardStatus resolve(
            String accountStatus,
            String deviceStatus
    ) {
        return NagraSmartcardStatusMapper.resolve(
                accountStatus,
                deviceStatus,
                ENDPOINT
        );
    }
}