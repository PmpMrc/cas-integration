package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdmBundledAccountPairingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReadCaSnFromPreloadRequest() throws Exception {
        String json = "{"
                + "\"sn\":\"109687603246\","
                + "\"smartcardType\":\"TIVU_HD_PAIRING\","
                + "\"source\":\"PHYSICAL\","
                + "\"productId\":\"22\","
                + "\"caSn\":\"1234567890\""
                + "}";

        PreloadSmartcardRequest request = objectMapper.readValue(
                json,
                PreloadSmartcardRequest.class
        );

        assertEquals("1234567890", request.caSn());
    }

    @Test
    void shouldSerializeCaSnInsideBundledDevice() {
        AdmBundledAccountRequest request =
                AdmBundledAccountRequest.preload(
                        "109687603246_TivuHDPairing",
                        "1096876032",
                        "1234567890"
                );

        JsonNode json = objectMapper.valueToTree(request);
        JsonNode device = json.path("devices").get(0);

        assertEquals("1234567890", device.path("caSN").asText());
        assertFalse(device.has("caSn"));
        assertFalse(json.has("caSN"));
        assertEquals("DISABLED", device.path("status").asText());
        assertEquals(
                "109687603246_TivuHDPairing",
                device.path("_id").asText()
        );
    }

    @Test
    void shouldOmitCaSnWhenNotProvided() {
        AdmBundledAccountRequest request =
                AdmBundledAccountRequest.preload(
                        "109687603246_TivuHD",
                        "1096876032"
                );

        JsonNode json = objectMapper.valueToTree(request);
        JsonNode device = json.path("devices").get(0);

        assertFalse(device.has("caSN"));
        assertFalse(device.has("caSn"));
        assertEquals("ACTIVE", json.path("status").asText());
        assertEquals("DISABLED", device.path("status").asText());
    }
}