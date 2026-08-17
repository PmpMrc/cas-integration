package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tivusat.cas.application.NagraOperationLogService;
import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.AdmAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
        import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class NagraRestExecutorTest {

    private static final String SN = "109687603246";

    @Mock
    private NagraOperationLogService operationLogService;

    private MockRestServiceServer server;
    private NagraRestExecutor executor;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:9090");

        server = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.build();

        executor = new NagraRestExecutor(
                restClient,
                nagraProperties(),
                new ObjectMapper(),
                operationLogService
        );
    }

    @Test
    void shouldExecuteRequestAndLogSuccess() {
        AdmAccountRequest body = AdmAccountRequest.active(SN);

        server.expect(requestTo("http://localhost:9090/adm/v1/accounts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("nv-source-id", "1000"))
                .andExpect(header("nv-broadcast-mode", "W"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                {
                  "_id": "109687603246",
                  "status": "ACTIVE",
                  "billingAddress": [],
                  "suspensionMode": "MOP"
                }
                """))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"result\":\"ADM account created\"}"));

        assertDoesNotThrow(() -> executor.execute(
                NagraOperation.ADM_CREATE_ACCOUNT,
                SN,
                HttpMethod.POST,
                "/adm/v1/accounts",
                SmartcardSource.PHYSICAL,
                "W",
                body
        ));

        verify(operationLogService).logSuccess(
                eq(SN),
                eq(NagraOperation.ADM_CREATE_ACCOUNT),
                eq("POST"),
                eq("/adm/v1/accounts"),
                contains("\"_id\":\"109687603246\""),
                eq("{\"result\":\"ADM account created\"}"),
                eq(201),
                anyLong()
        );

        server.verify();
    }

    @Test
    void shouldLogErrorAndThrowExceptionWhenNagraReturnsHttpError() {
        AdmAccountRequest body = AdmAccountRequest.active(SN);

        server.expect(requestTo("http://localhost:9090/adm/v1/accounts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("nv-source-id", "1000"))
                .andExpect(header("nv-broadcast-mode", "W"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"boom\"}"));

        NagraException exception = assertThrows(
                NagraException.class,
                () -> executor.execute(
                        NagraOperation.ADM_CREATE_ACCOUNT,
                        SN,
                        HttpMethod.POST,
                        "/adm/v1/accounts",
                        SmartcardSource.PHYSICAL,
                        "W",
                        body
                )
        );

        assertEquals(500, exception.getHttpStatus());
        assertTrue(exception.getResponseBody().contains("boom"));

        verify(operationLogService).logError(
                eq(SN),
                eq(NagraOperation.ADM_CREATE_ACCOUNT),
                eq("POST"),
                eq("/adm/v1/accounts"),
                contains("\"_id\":\"109687603246\""),
                contains("boom"),
                eq(500),
                isNull(),
                contains("500"),
                anyLong()
        );

        server.verify();
    }

    @Test
    void shouldLogTechnicalErrorAndThrowExceptionWhenRequestFails() {
        RestClient failingRestClient = RestClient.builder()
                .baseUrl("http://localhost:9090")
                .requestFactory(failingRequestFactory())
                .build();

        NagraRestExecutor failingExecutor = new NagraRestExecutor(
                failingRestClient,
                nagraProperties(),
                new ObjectMapper(),
                operationLogService
        );

        AdmAccountRequest body = AdmAccountRequest.active(SN);

        NagraException exception = assertThrows(
                NagraException.class,
                () -> failingExecutor.execute(
                        NagraOperation.ADM_CREATE_ACCOUNT,
                        SN,
                        HttpMethod.POST,
                        "/adm/v1/accounts",
                        SmartcardSource.PHYSICAL,
                        "W",
                        body
                )
        );

        assertNull(exception.getHttpStatus());
        assertNotNull(exception.getResponseBody());

        verify(operationLogService).logError(
                eq(SN),
                eq(NagraOperation.ADM_CREATE_ACCOUNT),
                eq("POST"),
                eq("/adm/v1/accounts"),
                contains("\"_id\":\"109687603246\""),
                isNull(),
                isNull(),
                eq("TECHNICAL_ERROR"),
                contains("network down"),
                anyLong()
        );
    }

    private ClientHttpRequestFactory failingRequestFactory() {
        return (uri, httpMethod) -> {
            throw new IOException("network down");
        };
    }

    private NagraProperties nagraProperties() {
        return new NagraProperties(
                true,
                "http://localhost:9090",
                new NagraProperties.SourceId(
                        "1000",
                        "2000"
                ),
                new NagraProperties.Paths(
                        "/adm/v1/accounts",
                        "/rmg/v1/entitlements",
                        "/adm/v1/devices",
                        "/v1/devices/{deviceId}",
                        "/adm/v1/devices/{deviceId}",
                        "/rmg/v1/operator/entitlements"
                )
        );
    }
}