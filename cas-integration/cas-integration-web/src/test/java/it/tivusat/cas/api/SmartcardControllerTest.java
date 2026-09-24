package it.tivusat.cas.api;

import it.tivusat.cas.api.dto.ImportSmartcardsResponse;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.application.PreloadSmartcardUseCase;
import it.tivusat.cas.application.SmartcardImportService;
import it.tivusat.cas.application.SmartcardOperationsUseCase;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.infrastructure.nagra.EntitlementValidationException;
import it.tivusat.cas.infrastructure.nagra.NagraException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SmartcardController.class)
class SmartcardControllerTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PreloadSmartcardUseCase preloadSmartcardUseCase;

    @MockBean
    private SmartcardOperationsUseCase smartcardOperationsUseCase;

    @MockBean
    private SmartcardImportService smartcardImportService;

    @Test
    void preloadShouldReturnAccepted() throws Exception {
        when(preloadSmartcardUseCase.preload(any()))
                .thenReturn(response(SmartcardStatus.PRELOADED));

        String body = "{"
                + "\"sn\":\"" + SN + "\","
                + "\"smartcardType\":\"TIVU_HD\","
                + "\"source\":\"PHYSICAL\","
                + "\"productId\":\"22\""
                + "}";

        mockMvc.perform(post("/api/v1/smartcards/preload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sn", is(SN)))
                .andExpect(jsonPath("$.ua", is(UA)))
                .andExpect(jsonPath("$.status", is("PRELOADED")));
    }

    @Test
    void activateShouldReturnAccepted() throws Exception {
        when(smartcardOperationsUseCase.activate(eq(SN), any()))
                .thenReturn(response(SmartcardStatus.ENABLED));

        String body = "{"
                + "\"source\":\"PHYSICAL\","
                + "\"smartcardType\":\"TIVU_HD\","
                + "\"caSn\":null"
                + "}";

        mockMvc.perform(post("/api/v1/smartcards/" + SN + "/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("ENABLED")));
    }

    @Test
    void activateShouldExplainWhyDeviceWasNotCreated() throws Exception {
        when(smartcardOperationsUseCase.activate(eq(SN), any()))
                .thenThrow(EntitlementValidationException.notFound(SN));

        mockMvc.perform(post("/api/v1/smartcards/" + SN + "/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"PHYSICAL\",\"smartcardType\":\"TIVU_HD\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("INVALID_SMARTCARD_STATE")))
                .andExpect(jsonPath("$.details.reason", is("NOT_FOUND")))
                .andExpect(jsonPath("$.details.operation", is("RMG_GET_ENTITLEMENTS")))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("ADM device was not created")));
    }

    @Test
    void preloadShouldExposeNagraOperationAndErrorCode() throws Exception {
        when(preloadSmartcardUseCase.preload(any())).thenThrow(new NagraException(
                400, "{\"code\":400,\"errorCode\":1001,\"message\":\"Generic bad request\"}",
                null, NagraOperation.RMG_CREATE_ENTITLEMENT,
                "/rmg/v1/operator/entitlements", NagraException.FailureType.HTTP_ERROR,
                "{\"accountId\":\"" + SN + "\",\"productId\":\"22\"}"
        ));

        mockMvc.perform(post("/api/v1/smartcards/preload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sn\":\"" + SN + "\",\"source\":\"PHYSICAL\","
                                + "\"smartcardType\":\"TIVU_HD\",\"productId\":\"22\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error", is("NAGRA_ERROR")))
                .andExpect(jsonPath("$.details.operation", is("RMG_CREATE_ENTITLEMENT")))
                .andExpect(jsonPath("$.details.upstreamHttpStatus", is("400")))
                .andExpect(jsonPath("$.details.upstreamErrorCode", is("1001")))
                .andExpect(jsonPath("$.details.upstreamMessage", is("Generic bad request")))
                .andExpect(jsonPath("$.details.requestPayload", org.hamcrest.Matchers.containsString("productId")))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("RMG_CREATE_ENTITLEMENT")));
    }

    @Test
    void refreshShouldReturnAccepted() throws Exception {
        when(smartcardOperationsUseCase.refresh(eq(SN), any()))
                .thenReturn(response(SmartcardStatus.ENABLED));

        String body = "{"
                + "\"source\":\"PHYSICAL\","
                + "\"smartcardType\":\"TIVU_HD\","
                + "\"caSn\":null"
                + "}";

        mockMvc.perform(post("/api/v1/smartcards/" + SN + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("ENABLED")));
    }

    @Test
    void suspendShouldReturnAccepted() throws Exception {
        when(smartcardOperationsUseCase.suspend(eq(SN), eq(SmartcardSource.PHYSICAL)))
                .thenReturn(response(SmartcardStatus.DISABLED));

        mockMvc.perform(post("/api/v1/smartcards/" + SN + "/suspend")
                        .param("source", "PHYSICAL"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("DISABLED")));
    }

    @Test
    void deleteShouldReturnAccepted() throws Exception {
        when(smartcardOperationsUseCase.delete(eq(SN), eq(SmartcardSource.PHYSICAL)))
                .thenReturn(response(SmartcardStatus.DELETED));

        mockMvc.perform(delete("/api/v1/smartcards/" + SN)
                        .param("source", "PHYSICAL"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("DELETED")));
    }

    @Test
    void statusShouldReturnOk() throws Exception {
        when(smartcardOperationsUseCase.getStatus(eq(SN), eq(SmartcardSource.PHYSICAL)))
                .thenReturn(response(SmartcardStatus.ENABLED));

        mockMvc.perform(get("/api/v1/smartcards/" + SN + "/status")
                        .param("source", "PHYSICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ENABLED")));
    }

    @Test
    void importShouldReturnOk() throws Exception {
        ImportSmartcardsResponse importResponse = new ImportSmartcardsResponse(
                "file.csv",
                1,
                1,
                0,
                Collections.emptyList()
        );

        when(smartcardImportService.importFile(any()))
                .thenReturn(importResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "file.csv",
                "text/csv",
                "sn,source,smartcardType,productId\n109687603246,PHYSICAL,TIVU_HD,22\n".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/smartcards/import")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName", is("file.csv")))
                .andExpect(jsonPath("$.totalRows", is(1)))
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.failureCount", is(0)));
    }

    @Test
    void activateWithoutBodyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/smartcards/" + SN + "/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusWithoutSourceShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/smartcards/" + SN + "/status"))
                .andExpect(status().isBadRequest());
    }

    private SmartcardResponse response(SmartcardStatus status) {
        return new SmartcardResponse(
                SN,
                UA,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                status,
                status == SmartcardStatus.PRELOADED,
                status == SmartcardStatus.ENABLED || status == SmartcardStatus.DISABLED,
                "TivuHD",
                "22",
                null,
                Instant.parse("2030-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
