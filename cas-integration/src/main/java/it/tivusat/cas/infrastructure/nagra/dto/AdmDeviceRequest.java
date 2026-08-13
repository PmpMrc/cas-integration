package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdmDeviceRequest(
        @JsonProperty("_id")
        String id,

        String status,
        String caSN,
        String smartCardId,
        String accountUid
) {

    public static AdmDeviceRequest activate(
            String sn,
            String ua,
            String caSn
    ) {
        return new AdmDeviceRequest(
                sn,
                "ENABLED",
                caSn,
                ua,
                sn
        );
    }

    public static AdmDeviceRequest refresh(
            String sn,
            String ua,
            String caSn
    ) {
        return new AdmDeviceRequest(
                null,
                "ENABLED",
                caSn,
                ua,
                sn
        );
    }

    public static AdmDeviceRequest suspend() {
        return new AdmDeviceRequest(
                null,
                "DISABLED",
                null,
                null,
                null
        );
    }
}