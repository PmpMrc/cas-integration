package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ImportSmartcardsResponse;
import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.domain.SmartcardFamily;
import it.tivusat.cas.domain.SmartcardRoutingTarget;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.exception.UnsupportedSmartcardRangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartcardImportServiceTest {

    @Mock
    private PreloadSmartcardUseCase preloadSmartcardUseCase;

    private SmartcardImportService service;

    @BeforeEach
    void setUp() {
        service = new SmartcardImportService(preloadSmartcardUseCase);
    }

    @Test
    void importSmartcardsShouldReturnRowByRowReport() {
        String csv = """
            sn,smartcardType,source,productId
            109687603246,TIVU_HD,PHYSICAL,PRODUCT_TEST
            109884211222,TIVU_VIRTUAL,VIRTUAL,PRODUCT_TEST
            109202636869,TIVU_HD,PHYSICAL,PRODUCT_TEST
            123,BAD_TYPE,PHYSICAL,PRODUCT_TEST
            """;

        doAnswer(invocation -> {
            PreloadSmartcardRequest request = invocation.getArgument(0);

            if ("109202636869".equals(request.sn())) {
                throw new UnsupportedSmartcardRangeException(
                        "1092026368",
                        SmartcardFamily.TIGER,
                        SmartcardRoutingTarget.LEGACY_SOA_SMS
                );
            }

            return null;
        })
                .when(preloadSmartcardUseCase)
                .preload(any(PreloadSmartcardRequest.class));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "smartcards-import.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ImportSmartcardsResponse response = service.importSmartcards(file);

        assertThat(response.totalRows()).isEqualTo(4);
        assertThat(response.successRows()).isEqualTo(2);
        assertThat(response.failedRows()).isEqualTo(2);
        assertThat(response.results()).hasSize(4);

        assertThat(response.results().get(0).rowNumber()).isEqualTo(2);
        assertThat(response.results().get(0).sn()).isEqualTo("109687603246");
        assertThat(response.results().get(0).success()).isTrue();

        assertThat(response.results().get(1).rowNumber()).isEqualTo(3);
        assertThat(response.results().get(1).sn()).isEqualTo("109884211222");
        assertThat(response.results().get(1).success()).isTrue();

        assertThat(response.results().get(2).rowNumber()).isEqualTo(4);
        assertThat(response.results().get(2).sn()).isEqualTo("109202636869");
        assertThat(response.results().get(2).success()).isFalse();
        assertThat(response.results().get(2).error()).isEqualTo("UNSUPPORTED_SMARTCARD_RANGE");

        assertThat(response.results().get(3).rowNumber()).isEqualTo(5);
        assertThat(response.results().get(3).sn()).isEqualTo("123");
        assertThat(response.results().get(3).success()).isFalse();
        assertThat(response.results().get(3).error()).isEqualTo("INVALID_ROW");

        ArgumentCaptor<PreloadSmartcardRequest> captor =
                ArgumentCaptor.forClass(PreloadSmartcardRequest.class);

        verify(preloadSmartcardUseCase, times(3)).preload(captor.capture());

        List<PreloadSmartcardRequest> requests = captor.getAllValues();

        assertThat(requests.get(0).sn()).isEqualTo("109687603246");
        assertThat(requests.get(0).smartcardType()).isEqualTo(SmartcardType.TIVU_HD);
        assertThat(requests.get(0).source()).isEqualTo(SmartcardSource.PHYSICAL);
        assertThat(requests.get(0).productId()).isEqualTo("PRODUCT_TEST");

        assertThat(requests.get(1).sn()).isEqualTo("109884211222");
        assertThat(requests.get(1).smartcardType()).isEqualTo(SmartcardType.TIVU_VIRTUAL);
        assertThat(requests.get(1).source()).isEqualTo(SmartcardSource.VIRTUAL);
        assertThat(requests.get(1).productId()).isEqualTo("PRODUCT_TEST");

        assertThat(requests.get(2).sn()).isEqualTo("109202636869");
    }

    @Test
    void importSmartcardsShouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        assertThatThrownBy(() -> service.importSmartcards(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Import file is mandatory and cannot be empty");

        verifyNoInteractions(preloadSmartcardUseCase);
    }

    @Test
    void importSmartcardsShouldSupportSemicolonSeparator() {
        String csv = """
            sn;smartcardType;source;productId
            109687603246;TIVU_HD;PHYSICAL;PRODUCT_TEST
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "smartcards-import.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ImportSmartcardsResponse response = service.importSmartcards(file);

        assertThat(response.totalRows()).isEqualTo(1);
        assertThat(response.successRows()).isEqualTo(1);
        assertThat(response.failedRows()).isZero();

        ArgumentCaptor<PreloadSmartcardRequest> captor =
                ArgumentCaptor.forClass(PreloadSmartcardRequest.class);

        verify(preloadSmartcardUseCase).preload(captor.capture());

        assertThat(captor.getValue().sn()).isEqualTo("109687603246");
        assertThat(captor.getValue().smartcardType()).isEqualTo(SmartcardType.TIVU_HD);
        assertThat(captor.getValue().source()).isEqualTo(SmartcardSource.PHYSICAL);
        assertThat(captor.getValue().productId()).isEqualTo("PRODUCT_TEST");
    }
}