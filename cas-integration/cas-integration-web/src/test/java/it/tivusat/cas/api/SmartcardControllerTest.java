package it.tivusat.cas.api;

import it.tivusat.cas.application.PreloadSmartcardUseCase;
import it.tivusat.cas.application.SmartcardOperationsUseCase;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.exception.SmartcardNotFoundException;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import it.tivusat.cas.api.dto.ImportSmartcardRowResult;
import it.tivusat.cas.api.dto.ImportSmartcardsResponse;
import it.tivusat.cas.application.SmartcardImportService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;




@WebMvcTest(SmartcardController.class)
class SmartcardControllerTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreloadSmartcardUseCase preloadSmartcardUseCase;

    @MockitoBean
    private SmartcardOperationsUseCase smartcardOperationsUseCase;

    @MockitoBean
    private SmartcardImportService smartcardImportService;

    @Test
    void shouldPreloadSmartcard() throws Exception {
        doNothing()
                .when(preloadSmartcardUseCase)
                .preload(any());

        mockMvc.perform(post("/api/v1/smartcards/preload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "sn": "109687603246",
                      "smartcardType": "TIVU_HD",
                      "source": "PHYSICAL",
                      "productId": "PRODUCT_TEST"
                    }
                    """))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldActivateSmartcard() throws Exception {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markEnabled(null);

        when(smartcardOperationsUseCase.activate(eq(SN), any()))
                .thenReturn(smartcard);

        mockMvc.perform(post("/api/v1/smartcards/{sn}/activate", SN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "caSn": null
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sn").value(SN))
                .andExpect(jsonPath("$.ua").value(UA))
                .andExpect(jsonPath("$.smartcardType").value("TIVU_HD"))
                .andExpect(jsonPath("$.source").value("PHYSICAL"))
                .andExpect(jsonPath("$.status").value("ENABLED"))
                .andExpect(jsonPath("$.deviceCreated").value(true));
    }

    @Test
    void shouldSuspendSmartcard() throws Exception {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markDisabled();

        when(smartcardOperationsUseCase.suspend(SN))
                .thenReturn(smartcard);

        mockMvc.perform(post("/api/v1/smartcards/{sn}/suspend", SN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sn").value(SN))
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void shouldRefreshSmartcard() throws Exception {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markRefreshed(null);

        when(smartcardOperationsUseCase.refresh(eq(SN), any()))
                .thenReturn(smartcard);

        mockMvc.perform(post("/api/v1/smartcards/{sn}/refresh", SN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "caSn": null
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sn").value(SN))
                .andExpect(jsonPath("$.status").value("ENABLED"));
    }

    @Test
    void shouldDeleteSmartcard() throws Exception {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markDeleted();

        when(smartcardOperationsUseCase.delete(SN))
                .thenReturn(smartcard);

        mockMvc.perform(delete("/api/v1/smartcards/{sn}", SN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sn").value(SN))
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    void shouldGetSmartcardStatus() throws Exception {
        SmartcardEntity smartcard = createPreloadedSmartcard();

        when(smartcardOperationsUseCase.getStatus(SN))
                .thenReturn(smartcard);

        mockMvc.perform(get("/api/v1/smartcards/{sn}/status", SN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sn").value(SN))
                .andExpect(jsonPath("$.ua").value(UA))
                .andExpect(jsonPath("$.status").value("PRELOADED"));
    }

    @Test
    void shouldReturn404WhenSmartcardDoesNotExist() throws Exception {
        String missingSn = "999999999999";

        when(smartcardOperationsUseCase.getStatus(missingSn))
                .thenThrow(new SmartcardNotFoundException(missingSn));

        mockMvc.perform(get("/api/v1/smartcards/{sn}/status", missingSn))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("SMARTCARD_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Smartcard not found: 999999999999"))
                .andExpect(jsonPath("$.path").value("/api/v1/smartcards/999999999999/status"));
    }

    @Test
    void shouldReturn400WhenPreloadRequestIsInvalid() throws Exception {
        doThrow(new IllegalArgumentException("Invalid smartcard checksum"))
                .when(preloadSmartcardUseCase)
                .preload(any());

        mockMvc.perform(post("/api/v1/smartcards/preload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "sn": "109687603200",
                      "smartcardType": "TIVU_HD",
                      "source": "PHYSICAL",
                      "productId": "PRODUCT_TEST"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid smartcard checksum"))
                .andExpect(jsonPath("$.path").value("/api/v1/smartcards/preload"));
    }

    @Test
    void shouldReturn409WhenSmartcardStateIsInvalid() throws Exception {
        when(smartcardOperationsUseCase.activate(eq(SN), any()))
                .thenThrow(new IllegalStateException("Cannot activate a deleted smartcard"));

        mockMvc.perform(post("/api/v1/smartcards/{sn}/activate", SN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "caSn": null
                    }
                    """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("INVALID_SMARTCARD_STATE"))
                .andExpect(jsonPath("$.message").value("Cannot activate a deleted smartcard"))
                .andExpect(jsonPath("$.path").value("/api/v1/smartcards/109687603246/activate"));
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/smartcards/preload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "sn": "109687603246",
                      "smartcardType": "TIVU_HD",
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
                .andExpect(jsonPath("$.path").value("/api/v1/smartcards/preload"));
    }

    @Test
    void shouldReturn400WhenMandatoryFieldIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/smartcards/preload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "sn": "109687603246",
                      "source": "PHYSICAL",
                      "productId": "PRODUCT_TEST"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.smartcardType").exists());
    }

    private SmartcardEntity createPreloadedSmartcard() {
        SmartcardEntity smartcard = new SmartcardEntity(
                SN,
                UA,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );

        smartcard.markPreloaded(
                SmartcardType.TIVU_HD.getNagraType(),
                "PRODUCT_TEST",
                Instant.now().plusSeconds(60L * 60 * 24 * 365 * 4)
        );

        return smartcard;
    }

    @Test
    void importSmartcardsShouldReturnAcceptedWithReport() throws Exception {
        ImportSmartcardsResponse response = new ImportSmartcardsResponse(
                2,
                1,
                1,
                List.of(
                        ImportSmartcardRowResult.success(2, "109687603246"),
                        ImportSmartcardRowResult.error(
                                3,
                                "109202636869",
                                "UNSUPPORTED_SMARTCARD_RANGE",
                                "Smartcard UA 1092026368 must be handled by legacy SOA/SMS"
                        )
                )
        );

        when(smartcardImportService.importSmartcards(any(MultipartFile.class)))
                .thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "smartcards-import.csv",
                "text/csv",
                """
                    sn,smartcardType,source,productId
                    109687603246,TIVU_HD,PHYSICAL,PRODUCT_TEST
                    """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(
                        multipart("/api/v1/smartcards/import")
                                .file(file)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.successRows").value(1))
                .andExpect(jsonPath("$.failedRows").value(1))
                .andExpect(jsonPath("$.results[0].success").value(true))
                .andExpect(jsonPath("$.results[1].error").value("UNSUPPORTED_SMARTCARD_RANGE"));
    }

    @Test
    void syncStatusShouldReturnUpdatedSmartcard() throws Exception {
        String sn = "109687603246";

        SmartcardEntity smartcard = new SmartcardEntity(
                sn,
                "1096876032",
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );

        smartcard.markPreloaded(
                "TivuHD",
                "PRODUCT_TEST",
                Instant.parse("2030-01-01T00:00:00Z")
        );

        smartcard.markEnabled("CA123456");

        when(smartcardOperationsUseCase.syncStatus(sn))
                .thenReturn(smartcard);

        mockMvc.perform(post("/api/v1/smartcards/{sn}/sync-status", sn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sn").value(sn))
                .andExpect(jsonPath("$.ua").value("1096876032"))
                .andExpect(jsonPath("$.status").value("ENABLED"))
                .andExpect(jsonPath("$.deviceCreated").value(true))
                .andExpect(jsonPath("$.caSn").value("CA123456"));
    }
}