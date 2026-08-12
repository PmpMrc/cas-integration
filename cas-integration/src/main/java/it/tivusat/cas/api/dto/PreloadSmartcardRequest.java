package it.tivusat.cas.api.dto;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreloadSmartcardRequest(
        @NotBlank
        String sn,

        @NotNull
        SmartcardType smartcardType,

        @NotNull
        SmartcardSource source,

        @NotBlank
        String productId
) {
}