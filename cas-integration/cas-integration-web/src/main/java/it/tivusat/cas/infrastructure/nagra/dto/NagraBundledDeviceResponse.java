package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NagraBundledDeviceResponse {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("smartCardId")
    private String smartCardId;

    @JsonProperty("caSN")
    private String caSn;

    @JsonProperty("device_status")
    private String status;

    @JsonProperty("device_created")
    private Instant createdAt;

    @JsonProperty("device_modified")
    private Instant updatedAt;

    @JsonProperty("SCSetID")
    private String scSetId;

    public NagraBundledDeviceResponse() {
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSmartCardId() {
        return smartCardId;
    }

    public String getCaSn() {
        return caSn;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getScSetId() {
        return scSetId;
    }
}