package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.AdmAccountRequest;
import it.tivusat.cas.infrastructure.nagra.dto.AdmDeviceRequest;
import it.tivusat.cas.infrastructure.nagra.dto.NagraDeviceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

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
class NagraAdmClientTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";

    @Mock
    private NagraRestExecutor restExecutor;

    @Mock
    private NagraProperties properties;

    @Mock
    private NagraProperties.Paths paths;

    private NagraAdmClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().when(properties.paths()).thenReturn(paths);

        lenient().when(paths.createAccount()).thenReturn("/adm/v1/accounts");
        lenient().when(paths.createDevice()).thenReturn("/adm/v1/devices");
        lenient().when(paths.updateDevice()).thenReturn("/v1/devices/{deviceId}");
        lenient().when(paths.getDevice()).thenReturn("/adm/v1/devices/{deviceId}");

        client = new NagraAdmClient(properties, restExecutor);
    }

    @Test
    void createAccountShouldCallAdmCreateAccountEndpoint() {
        AdmAccountRequest request = AdmAccountRequest.active(SN);

        client.createAccount(SmartcardSource.PHYSICAL, request);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.ADM_CREATE_ACCOUNT, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.POST, args[2]);
        assertEquals("/adm/v1/accounts", args[3]);
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("W", args[5]);
        assertSame(request, args[6]);
    }

    @Test
    void createDeviceShouldCallAdmCreateDeviceEndpoint() {
        AdmDeviceRequest request = AdmDeviceRequest.activate(SN, UA, null);

        client.createDevice(SmartcardSource.PHYSICAL, request);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.ADM_CREATE_DEVICE, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.POST, args[2]);
        assertEquals("/adm/v1/devices", args[3]);
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("N", args[5]);
        assertSame(request, args[6]);
    }

    @Test
    void updateDeviceForRefreshShouldCallAdmUpdateDeviceEndpointWithBroadcastOne() {
        AdmDeviceRequest request = AdmDeviceRequest.refresh(SN, UA, null);

        client.updateDeviceForRefresh(SmartcardSource.PHYSICAL, SN, request);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.ADM_REFRESH_DEVICE, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.PUT, args[2]);
        assertEndpoint(args[3], "/v1/devices/{deviceId}", "/v1/devices/" + SN);
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("1", args[5]);
        assertSame(request, args[6]);
        assertUriVariableIfPresent(args, 7, SN);
    }

    @Test
    void refreshPayloadShouldNotContainId() throws Exception {
        AdmDeviceRequest request = AdmDeviceRequest.refresh(SN, UA, null);

        String json = objectMapper.writeValueAsString(request);

        assertFalse(json.contains("\"_id\""), "Refresh body must not contain _id");
        assertTrue(json.contains("\"status\":\"ENABLED\""));
        assertTrue(json.contains("\"smartCardId\":\"" + UA + "\""));
        assertTrue(json.contains("\"accountUid\":\"" + SN + "\""));
    }

    @Test
    void suspendDeviceShouldCallAdmUpdateDeviceEndpointWithDisabledBody() throws Exception {
        client.suspendDevice(SmartcardSource.PHYSICAL, SN);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.ADM_SUSPEND_DEVICE, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.PUT, args[2]);
        assertEndpoint(args[3], "/v1/devices/{deviceId}", "/v1/devices/" + SN);
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("1", args[5]);

        AdmDeviceRequest request = (AdmDeviceRequest) args[6];
        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"status\":\"DISABLED\""));
        assertFalse(json.contains("\"_id\""));
        assertFalse(json.contains("\"smartCardId\""));
        assertFalse(json.contains("\"accountUid\""));

        assertUriVariableIfPresent(args, 7, SN);
    }

    @Test
    void deleteDeviceShouldCallAdmDeleteDeviceEndpointWithCancelIccTrue() {
        client.deleteDevice(SmartcardSource.PHYSICAL, SN);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.ADM_DELETE_DEVICE, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.DELETE, args[2]);
        assertEndpoint(args[3], "/v1/devices/{deviceId}?cancelICC=true", "/v1/devices/" + SN + "?cancelICC=true");
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("1", args[5]);
        assertEquals(null, args[6]);

        assertUriVariableIfPresent(args, 7, SN);
    }

    @Test
    void getDeviceShouldCallAdmGetDeviceEndpoint() {
        NagraDeviceResponse expectedResponse = mock(NagraDeviceResponse.class);

        lenient().when(restExecutor.<NagraDeviceResponse>execute(
                any(NagraOperation.class),
                anyString(),
                any(HttpMethod.class),
                anyString(),
                any(SmartcardSource.class),
                anyString(),
                isNull(),
                eq(NagraDeviceResponse.class)
        )).thenReturn(expectedResponse);

        lenient().when(restExecutor.<NagraDeviceResponse>execute(
                any(NagraOperation.class),
                anyString(),
                any(HttpMethod.class),
                anyString(),
                any(SmartcardSource.class),
                anyString(),
                isNull(),
                eq(NagraDeviceResponse.class),
                any()
        )).thenReturn(expectedResponse);

        NagraDeviceResponse response = client.getDevice(SmartcardSource.PHYSICAL, SN);

        assertSame(expectedResponse, response);

        Object[] args = singleInvocation().getArguments();

        assertEquals(NagraOperation.ADM_GET_DEVICE, args[0]);
        assertEquals(SN, args[1]);
        assertEquals(HttpMethod.GET, args[2]);
        assertEndpoint(args[3], "/adm/v1/devices/{deviceId}", "/adm/v1/devices/" + SN);
        assertEquals(SmartcardSource.PHYSICAL, args[4]);
        assertEquals("W", args[5]);
        assertEquals(null, args[6]);
        assertEquals(NagraDeviceResponse.class, args[7]);

        assertUriVariableIfPresent(args, 8, SN);
    }

    private Invocation singleInvocation() {
        Collection<Invocation> invocations = mockingDetails(restExecutor).getInvocations();

        assertEquals(1, invocations.size());

        return invocations.iterator().next();
    }

    private void assertEndpoint(Object actualEndpoint, String templateEndpoint, String expandedEndpoint) {
        String endpoint = String.valueOf(actualEndpoint);

        assertTrue(
                templateEndpoint.equals(endpoint) || expandedEndpoint.equals(endpoint),
                "Unexpected endpoint: " + endpoint
        );
    }

    private void assertUriVariableIfPresent(Object[] args, int firstVarArgIndex, String expectedValue) {
        if (args.length > firstVarArgIndex) {
            assertEquals(expectedValue, args[firstVarArgIndex]);
        }
    }
}