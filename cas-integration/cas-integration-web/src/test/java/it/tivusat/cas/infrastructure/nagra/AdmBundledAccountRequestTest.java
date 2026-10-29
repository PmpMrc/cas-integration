package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tivusat.cas.infrastructure.nagra.dto.AdmBundledAccountRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdmBundledAccountRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preloadPayloadShouldContainActiveAccountAndDisabledDevice()
            throws Exception {

        AdmBundledAccountRequest request =
                AdmBundledAccountRequest.preload(
                        "109687603246_TivuHD",
                        "1096876032"
                );

        String serialized = objectMapper.writeValueAsString(request);
        JsonNode actual = objectMapper.readTree(serialized);

        JsonNode expected = objectMapper.readTree(
                "{"
                        + "\"_id\":\"109687603246_TivuHD\","
                        + "\"status\":\"ACTIVE\","
                        + "\"billingAddress\":[],"
                        + "\"suspensionMode\":\"MOP\","
                        + "\"devices\":[{"
                        + "\"_id\":\"109687603246_TivuHD\","
                        + "\"status\":\"DISABLED\","
                        + "\"smartCardId\":\"1096876032\","
                        + "\"accountUid\":\"109687603246_TivuHD\""
                        + "}]"
                        + "}"
        );

        assertEquals(expected, actual);
    }
}