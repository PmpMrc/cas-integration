package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NagraRmgClientTest {

    private static final String SN = "109687603246";

    private NagraRestExecutor restExecutor;

    @Mock
    private NagraProperties properties;

    @Mock
    private NagraProperties.Paths paths;

    private NagraRmgClient client;

    private NagraEntitlementResponse mockedEntitlementResponse;

    @BeforeEach
    void setUp() {
        restExecutor = mock(NagraRestExecutor.class, invocation -> {
            if ("execute".equals(invocation.getMethod().getName())) {
                Object[] args = invocation.getArguments();

                if (args.length >= 8 && NagraEntitlementResponse.class.equals(args[7])) {
                    return mockedEntitlementResponse;
                }
            }

            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });

        lenient().when(properties.paths()).thenReturn(paths);

        lenient().when(paths.createEntitlement()).thenReturn("/rmg/v1/entitlements");
        lenient().when(paths.getEntitlements()).thenReturn("/rmg/v1/operator/entitlements");

        client = new NagraRmgClient(properties, restExecutor);
    }

    @Test
    void createEntitlementShouldCallRmgCreateEntitlementEndpoint() {
        RmgEntitlementRequest request = RmgEntitlementRequest.subscription(
                "TivuHD",
                SN,
                "PRODUCT_TEST",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2030-01-01T00:00:00Z")
        );

        client.createEntitlement(SmartcardSource.PHYSICAL, request);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.RMG_CREATE_ENTITLEMENT, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.POST, args[2]);
        assertEquals("/rmg/v1/entitlements", args[3]);
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("W", args[5]);
        assertSame(request, args[6]);
    }

    @Test
    void getEntitlementsByAccountIdShouldEncodeFilterAndReturnEntitlement() {
        NagraEntitlementResponse expectedResponse = mock(NagraEntitlementResponse.class);
        mockedEntitlementResponse = expectedResponse;

        NagraEntitlementResponse response = client.getEntitlementsByAccountId(
                SmartcardSource.PHYSICAL,
                SN
        );

        assertSame(expectedResponse, response);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.RMG_GET_ENTITLEMENTS, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.GET, args[2]);
        assertRmgFilterEndpoint(String.valueOf(args[3]));
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("W", args[5]);
        assertNull(args[6]);
        assertEquals(NagraEntitlementResponse.class, args[7]);
    }

    @Test
    void getEntitlementsByAccountIdShouldReturnNullWhenExecutorReturnsNull() {
        mockedEntitlementResponse = null;

        NagraEntitlementResponse response = client.getEntitlementsByAccountId(
                SmartcardSource.PHYSICAL,
                SN
        );

        assertNull(response);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.RMG_GET_ENTITLEMENTS, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.GET, args[2]);
        assertRmgFilterEndpoint(String.valueOf(args[3]));
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("W", args[5]);
        assertNull(args[6]);
        assertEquals(NagraEntitlementResponse.class, args[7]);
    }

    private Invocation singleInvocation() {
        Collection<Invocation> invocations = mockingDetails(restExecutor).getInvocations();

        assertEquals(1, invocations.size());

        return invocations.iterator().next();
    }

    private void assertRmgFilterEndpoint(String endpoint) {
        assertTrue(endpoint.startsWith("/rmg/v1/operator/entitlements?filter="));
        assertTrue(endpoint.contains("109687603246"));

        assertFalse(endpoint.contains("{\"accountId\""));
        assertFalse(endpoint.contains("{"));
        assertFalse(endpoint.contains("}"));
    }
}