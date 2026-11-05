package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.NagraBundledAccountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NagraBundledStatusGatewayTest {

    private static final String SN = "109687603246";
    private static final String ACCOUNT_ID = SN + "_TivuHD";
    private static final SmartcardSource SOURCE = SmartcardSource.PHYSICAL;

    private NagraAdmClient adm;
    private NagraRmgClient rmg;
    private NagraSmartcardGateway gateway;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        NagraProperties properties = mock(NagraProperties.class);
        NagraProperties.Paths paths = mock(NagraProperties.Paths.class);

        when(properties.enabled()).thenReturn(true);
        when(properties.paths()).thenReturn(paths);
        when(paths.getBundledAccount())
                .thenReturn("/adm/v1/bundled/accounts/{accountId}");

        adm = mock(NagraAdmClient.class);
        rmg = mock(NagraRmgClient.class);

        gateway = new NagraSmartcardGateway(properties, adm, rmg);
    }

    @Test
    void shouldSelectMatchingDeviceInsteadOfFirstDevice() throws Exception {
        NagraBundledAccountResponse account = read(
                "{\"_id\":\"" + ACCOUNT_ID + "\","
                        + "\"status\":\"ACTIVE\","
                        + "\"devices\":["
                        + "{\"deviceId\":\"another-device\","
                        + "\"device_status\":\"DISABLED\"},"
                        + "{\"deviceId\":\"" + ACCOUNT_ID + "\","
                        + "\"device_status\":\"ENABLED\"}"
                        + "]}"
        );

        when(adm.getBundledAccount(SOURCE, SN, ACCOUNT_ID))
                .thenReturn(account);

        NagraSmartcardStatusSnapshot snapshot = fetch();

        assertEquals(SmartcardStatus.ENABLED, snapshot.status());
        assertSame(account, snapshot.account());
        assertEquals(
                ACCOUNT_ID,
                snapshot.bundledDevice().getDeviceId()
        );

        InOrder order = inOrder(adm, rmg);
        order.verify(adm).getBundledAccount(SOURCE, SN, ACCOUNT_ID);
        order.verify(rmg).getEntitlementsByAccountId(
                SOURCE, SN, ACCOUNT_ID
        );

        verifyNoMoreInteractions(adm, rmg);
    }

    @Test
    void shouldAllowPreloadedAccountWithoutEntitlement() throws Exception {
        when(adm.getBundledAccount(SOURCE, SN, ACCOUNT_ID))
                .thenReturn(read(
                        "{\"_id\":\"" + ACCOUNT_ID + "\","
                                + "\"status\":\"ACTIVE\","
                                + "\"devices\":[{"
                                + "\"deviceId\":\"" + ACCOUNT_ID + "\","
                                + "\"device_status\":\"DISABLED\""
                                + "}]}"
                ));

        NagraSmartcardStatusSnapshot snapshot = fetch();

        assertEquals(SmartcardStatus.PRELOADED, snapshot.status());
        assertNull(snapshot.entitlement());

        verify(rmg).getEntitlementsByAccountId(SOURCE, SN, ACCOUNT_ID);
    }

    @Test
    void shouldRejectActiveAccountWithoutMatchingDevice() throws Exception {
        when(adm.getBundledAccount(SOURCE, SN, ACCOUNT_ID))
                .thenReturn(read(
                        "{\"_id\":\"" + ACCOUNT_ID + "\","
                                + "\"status\":\"ACTIVE\","
                                + "\"devices\":[]}"
                ));

        NagraException error = assertThrows(
                NagraException.class,
                this::fetch
        );

        assertEquals(
                NagraException.FailureType.INVALID_RESPONSE,
                error.getFailureType()
        );
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldRejectUnexpectedAccountId() throws Exception {
        when(adm.getBundledAccount(SOURCE, SN, ACCOUNT_ID))
                .thenReturn(read(
                        "{\"_id\":\"different-account\","
                                + "\"status\":\"CANCELLED\","
                                + "\"devices\":[]}"
                ));

        NagraException error = assertThrows(
                NagraException.class,
                this::fetch
        );

        assertEquals(
                NagraException.FailureType.INVALID_RESPONSE,
                error.getFailureType()
        );
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldNotQueryRmgWhenAdmFails() {
        NagraException failure =
                new NagraException(503, "Simulated ADM unavailable");

        when(adm.getBundledAccount(SOURCE, SN, ACCOUNT_ID))
                .thenThrow(failure);

        NagraException actual = assertThrows(
                NagraException.class,
                this::fetch
        );

        assertSame(failure, actual);
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldNotHideRmgFailureAsMissingEntitlement() throws Exception {
        when(adm.getBundledAccount(SOURCE, SN, ACCOUNT_ID))
                .thenReturn(read(
                        "{\"_id\":\"" + ACCOUNT_ID + "\","
                                + "\"status\":\"SUSPENDED\","
                                + "\"devices\":[]}"
                ));

        NagraException failure =
                new NagraException(503, "Simulated RMG unavailable");

        when(rmg.getEntitlementsByAccountId(SOURCE, SN, ACCOUNT_ID))
                .thenThrow(failure);

        NagraException actual = assertThrows(
                NagraException.class,
                this::fetch
        );

        assertSame(failure, actual);
    }

    private NagraBundledAccountResponse read(String json) throws Exception {
        return mapper.readValue(json, NagraBundledAccountResponse.class);
    }

    private NagraSmartcardStatusSnapshot fetch() {
        return gateway.fetchSmartcardStatus(
                SN,
                SmartcardType.TIVU_HD,
                SOURCE
        );
    }
}