package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NagraEntitlementResponseTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldReadDocumentedEntitlementResponse() throws Exception {
        String json = "{"
                + "\"_id\":\"109687603246_TivuHD\","
                + "\"accountId\":\"109687603246_TivuHD\","
                + "\"productId\":\"22\","
                + "\"status\":\"SUBSCRIBED\","
                + "\"validityType\":\"ABSOLUTE\","
                + "\"productType\":\"SUBSCRIPTION\","
                + "\"validityFrom\":\"2026-11-05T10:00:00Z\","
                + "\"expiryDate\":\"2030-11-05T10:00:00Z\","
                + "\"additionalField\":\"ignored\""
                + "}";

        NagraEntitlementResponse response = objectMapper.readValue(
                json,
                NagraEntitlementResponse.class
        );

        assertEquals("109687603246_TivuHD", response.id());
        assertEquals("109687603246_TivuHD", response.accountId());
        assertEquals("22", response.productId());
        assertEquals("SUBSCRIBED", response.status());
        assertEquals("ABSOLUTE", response.validityType());
        assertEquals("SUBSCRIPTION", response.productType());

        assertEquals(
                Instant.parse("2026-11-05T10:00:00Z"),
                response.validityFrom()
        );
        assertEquals(
                Instant.parse("2030-11-05T10:00:00Z"),
                response.expiryDate()
        );
    }

    @Test
    void shouldNotInventMissingDates() throws Exception {
        String json = "{"
                + "\"_id\":\"109687603246_TivuHD\","
                + "\"accountId\":\"109687603246_TivuHD\","
                + "\"productId\":\"22\""
                + "}";

        NagraEntitlementResponse response = objectMapper.readValue(
                json,
                NagraEntitlementResponse.class
        );

        assertNull(response.validityFrom());
        assertNull(response.expiryDate());
    }
}