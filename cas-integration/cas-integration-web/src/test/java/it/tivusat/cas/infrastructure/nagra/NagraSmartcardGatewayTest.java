package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.AdmAccountRequest;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NagraSmartcardGatewayTest {

    private static final String SN = "109687603246";
    private static final SmartcardSource SOURCE = SmartcardSource.PHYSICAL;

    @Mock private NagraAdmClient adm;
    @Mock private NagraRmgClient rmg;

    private NagraSmartcardGateway gateway;
    private Instant from;
    private Instant until;

    @BeforeEach
    void setUp() {
        NagraProperties properties = new NagraProperties();
        properties.setEnabled(true);
        gateway = new NagraSmartcardGateway(properties, adm, rmg);
        from = Instant.now();
        until = from.plus(1460, ChronoUnit.DAYS);
    }

    private void preload() {
        gateway.preloadSmartcard(SN, SmartcardType.TIVU_HD, SOURCE, "22", from, until);
    }

    @Test
    void retriesEntitlementAfterPartialPreload() {
        NagraException rmgFailure = new NagraException(503, "service unavailable");
        doThrow(rmgFailure).doNothing().when(rmg)
                .createEntitlement(eq(SOURCE), any(RmgEntitlementRequest.class));
        doNothing().doThrow(new NagraException(409, "Account already exists"))
                .when(adm).createAccount(eq(SOURCE), any(AdmAccountRequest.class));

        assertThrows(NagraException.class, this::preload);
        assertDoesNotThrow(this::preload);

        verify(adm, times(2)).createAccount(eq(SOURCE), any(AdmAccountRequest.class));
        verify(rmg, times(2)).createEntitlement(eq(SOURCE), any(RmgEntitlementRequest.class));
    }

    @Test
    void verifiesEntitlementWhenItsCreationWasAlreadyCompleted() {
        doThrow(new NagraException(409, "Duplicate entitlement"))
                .when(rmg).createEntitlement(eq(SOURCE), any(RmgEntitlementRequest.class));
        when(rmg.getEntitlementsByAccountId(SOURCE, SN)).thenReturn(entitlement("22"));

        assertDoesNotThrow(this::preload);
        verify(rmg).getEntitlementsByAccountId(SOURCE, SN);
    }

    @Test
    void rejectsDuplicateEntitlementWithAnotherProduct() {
        doThrow(new NagraException(409, "Duplicate entitlement"))
                .when(rmg).createEntitlement(eq(SOURCE), any(RmgEntitlementRequest.class));
        when(rmg.getEntitlementsByAccountId(SOURCE, SN)).thenReturn(entitlement("23"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::preload);
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("productId expected 22, got 23"));
    }

    @Test
    void doesNotIgnoreUnidentifiedAccountConflict() {
        NagraException conflict = new NagraException(409, "Other conflict");
        doThrow(conflict).when(adm)
                .createAccount(eq(SOURCE), any(AdmAccountRequest.class));

        assertThrows(NagraException.class, this::preload);
        verifyNoInteractions(rmg);
    }

    @Test
    void doesNotMarkPreloadCompleteOnRmgFailure() {
        doThrow(new NagraException(null, "timeout")).when(rmg)
                .createEntitlement(eq(SOURCE), any(RmgEntitlementRequest.class));

        assertThrows(NagraException.class, this::preload);
    }

    @Test
    void activationWithoutEntitlementReportsWhatWasMissingAndDoesNotCreateDevice() {
        EntitlementValidationException exception = assertThrows(
                EntitlementValidationException.class,
                () -> gateway.activateSmartcard(SN, "1096876032", SOURCE, null)
        );

        assertEquals("NOT_FOUND", exception.details().get("reason"));
        assertEquals("RMG_GET_ENTITLEMENTS", exception.details().get("operation"));
        verifyNoInteractions(adm);
    }

    @Test
    void activationWithAnotherAccountReportsActualAccountIdAndDoesNotCreateDevice() {
        when(rmg.getEntitlementsByAccountId(SOURCE, SN)).thenReturn(new NagraEntitlementResponse(
                SN + "_TivuHD", "999999999999", "22", "SUBSCRIBED", "ABSOLUTE",
                "SUBSCRIPTION", from, until
        ));

        EntitlementValidationException exception = assertThrows(
                EntitlementValidationException.class,
                () -> gateway.activateSmartcard(SN, "1096876032", SOURCE, null)
        );

        assertEquals("ACCOUNT_MISMATCH", exception.details().get("reason"));
        assertEquals("999999999999", exception.details().get("actualAccountId"));
        verifyNoInteractions(adm);
    }

    @Test
    void activationWithUnsubscribedEntitlementReportsActualStatusAndDoesNotCreateDevice() {
        when(rmg.getEntitlementsByAccountId(SOURCE, SN)).thenReturn(new NagraEntitlementResponse(
                SN + "_TivuHD", SN, "22", "CANCELLED", "ABSOLUTE",
                "SUBSCRIPTION", from, until
        ));

        EntitlementValidationException exception = assertThrows(
                EntitlementValidationException.class,
                () -> gateway.activateSmartcard(SN, "1096876032", SOURCE, null)
        );

        assertEquals("STATUS_NOT_SUBSCRIBED", exception.details().get("reason"));
        assertEquals("CANCELLED", exception.details().get("actualStatus"));
        verifyNoInteractions(adm);
    }

    private NagraEntitlementResponse entitlement(String productId) {
        return new NagraEntitlementResponse(
                SN + "_TivuHD", SN, productId, "SUBSCRIBED", "ABSOLUTE",
                "SUBSCRIPTION", from, until
        );
    }
}
