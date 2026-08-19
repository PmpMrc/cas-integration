package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ImportSmartcardsResponse;
import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.SmartcardValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmartcardImportServiceTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";

    private final PreloadSmartcardUseCase preloadSmartcardUseCase = mock(PreloadSmartcardUseCase.class);
    private final SmartcardValidator smartcardValidator = new SmartcardValidator();

    private final SmartcardImportService service = new SmartcardImportService(
            preloadSmartcardUseCase,
            smartcardValidator
    );

    @Test
    void shouldImportCsvWithSn() {
        when(preloadSmartcardUseCase.preload(any()))
                .thenReturn(response());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cards.csv",
                "text/csv",
                ("sn,source,smartcardType,productId\n"
                        + SN + ",PHYSICAL,TIVU_HD,PRODUCT_TEST\n").getBytes()
        );

        ImportSmartcardsResponse response = service.importFile(file);

        assertEquals("cards.csv", response.getFileName());
        assertEquals(1, response.getTotalRows());
        assertEquals(1, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertEquals(SN, response.getResults().get(0).getSn());
    }

    @Test
    void shouldImportCsvWithUaAndBuildSn() {
        when(preloadSmartcardUseCase.preload(any()))
                .thenReturn(response());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cards.csv",
                "text/csv",
                ("ua,source,smartcardType,productId\n"
                        + UA + ",PHYSICAL,TIVU_HD,PRODUCT_TEST\n").getBytes()
        );

        ImportSmartcardsResponse response = service.importFile(file);

        assertEquals(1, response.getSuccessCount());
        assertEquals(SN, response.getResults().get(0).getSn());
        assertEquals(UA, response.getResults().get(0).getUa());
    }

    @Test
    void shouldReturnFailureForInvalidRowAndContinue() {
        when(preloadSmartcardUseCase.preload(any()))
                .thenReturn(response());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cards.csv",
                "text/csv",
                ("sn,source,smartcardType,productId\n"
                        + SN + ",PHYSICAL,TIVU_HD,PRODUCT_TEST\n"
                        + "123,PHYSICAL,TIVU_HD,PRODUCT_TEST\n").getBytes()
        );

        ImportSmartcardsResponse response = service.importFile(file);

        assertEquals(2, response.getTotalRows());
        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
    }

    @Test
    void shouldFailIfHeaderIsMissing() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cards.csv",
                "text/csv",
                "".getBytes()
        );

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                service.importFile(file);
            }
        });
    }

    @Test
    void shouldPassValuesReadFromFileToPreloadUseCase() {
        when(preloadSmartcardUseCase.preload(any()))
                .thenReturn(response());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cards.csv",
                "text/csv",
                ("sn,source,smartcardType,productId\n"
                        + SN + ",PHYSICAL,TIVU_HD,PRODUCT_TEST\n").getBytes()
        );

        service.importFile(file);

        ArgumentCaptor<PreloadSmartcardRequest> captor = ArgumentCaptor.forClass(PreloadSmartcardRequest.class);

        org.mockito.Mockito.verify(preloadSmartcardUseCase).preload(captor.capture());

        assertEquals(SN, captor.getValue().sn());
        assertEquals(SmartcardSource.PHYSICAL, captor.getValue().source());
        assertEquals(SmartcardType.TIVU_HD, captor.getValue().smartcardType());
        assertEquals("PRODUCT_TEST", captor.getValue().productId());
    }

    private SmartcardResponse response() {
        return new SmartcardResponse(
                SN,
                UA,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                SmartcardStatus.PRELOADED,
                true,
                false,
                "TivuHD",
                "PRODUCT_TEST",
                null,
                Instant.parse("2030-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}