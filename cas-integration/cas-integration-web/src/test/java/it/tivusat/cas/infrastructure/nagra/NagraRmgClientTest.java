package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

@ExtendWith(MockitoExtension.class)
class NagraRmgClientTest {

    private static final String SN = "109687603246";

    @Mock
    private NagraRestExecutor restExecutor;

    @Mock
    private NagraProperties properties;

    @Mock
    private NagraProperties.Paths paths;

    private NagraRmgClient client;

    @BeforeEach
    void setUp() {
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
    void getEntitlementsByAccountIdShouldEncodeFilterAndReturnFirstEntitlement() {
        NagraEntitlementResponse expectedResponse = mock(NagraEntitlementResponse.class);

        lenient().when(restExecutor.<NagraEntitlementResponse[]>execute(
                any(NagraOperation.class),
                anyString(),
                any(HttpMethod.class),
                anyString(),
                any(SmartcardSource.class),
                anyString(),
                isNull(),
                eq(NagraEntitlementResponse[].class)
        )).thenReturn(new NagraEntitlementResponse[]{expectedResponse});

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
        assertEquals(null, args[6]);
        assertEquals(NagraEntitlementResponse[].class, args[7]);
    }

    @Test
    void getEntitlementsByAccountIdShouldReturnNullWhenRmgReturnsEmptyArray() {
        lenient().when(restExecutor.<NagraEntitlementResponse[]>execute(
                any(NagraOperation.class),
                anyString(),
                any(HttpMethod.class),
                anyString(),
                any(SmartcardSource.class),
                anyString(),
                isNull(),
                eq(NagraEntitlementResponse[].class)
        )).thenReturn(new NagraEntitlementResponse[0]);

        NagraEntitlementResponse response = client.getEntitlementsByAccountId(
                SmartcardSource.PHYSICAL,
                SN
        );

        assertEquals(null, response);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.RMG_GET_ENTITLEMENTS, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.GET, args[2]);
        assertRmgFilterEndpoint(String.valueOf(args[3]));
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("W", args[5]);
        assertEquals(null, args[6]);
        assertEquals(NagraEntitlementResponse[].class, args[7]);
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