package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NagraBundledAccountResponse {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created")
    private Instant createdAt;

    @JsonProperty("modified")
    private Instant updatedAt;

    @JsonProperty("devices")
    private List<NagraBundledDeviceResponse> devices;

    public NagraBundledAccountResponse() {
    }

    public String getId() {
        return id;
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

    public List<NagraBundledDeviceResponse> getDevices() {
        return devices;
    }
}