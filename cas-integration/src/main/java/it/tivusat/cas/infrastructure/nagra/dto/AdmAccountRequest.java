package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AdmAccountRequest(
        @JsonProperty("_id")
        String id,

        String status,

        List<Object> billingAddress,

        String suspensionMode
) {

    public static AdmAccountRequest active(String sn) {
        return new AdmAccountRequest(
                sn,
                "ACTIVE",
                List.of(),
                "MOP"
        );
    }
}