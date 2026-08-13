package it.tivusat.cas.domain;

import it.tivusat.cas.domain.exception.UnsupportedSmartcardRangeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UaRangeClassifierTest {

    private final UaRangeClassifier classifier = new UaRangeClassifier();

    @Test
    void shouldClassifyMerlinRangeAsNewRestInterface() {
        UaRange range = classifier.classify("1096876032");

        assertEquals(SmartcardFamily.MERLIN, range.family());
        assertEquals(SmartcardRoutingTarget.NEW_REST_INTERFACE, range.routingTarget());
    }

    @Test
    void shouldClassifyVirtualRangeAsNewRestInterface() {
        UaRange range = classifier.classify("1098842112");

        assertEquals(SmartcardFamily.VIRTUAL, range.family());
        assertEquals(SmartcardRoutingTarget.NEW_REST_INTERFACE, range.routingTarget());
    }

    @Test
    void shouldClassifyTigerRangeAsLegacySoaSms() {
        UaRange range = classifier.classify("1092026368");

        assertEquals(SmartcardFamily.TIGER, range.family());
        assertEquals(SmartcardRoutingTarget.LEGACY_SOA_SMS, range.routingTarget());
    }

    @Test
    void shouldClassifyUnknownRangeAsUnsupported() {
        UaRange range = classifier.classify("9999999999");

        assertEquals(SmartcardFamily.UNKNOWN, range.family());
        assertEquals(SmartcardRoutingTarget.UNSUPPORTED, range.routingTarget());
    }

    @Test
    void shouldAllowNewRestInterfaceRange() {
        assertDoesNotThrow(() -> classifier.validateRestSupported("1096876032"));
    }

    @Test
    void shouldRejectLegacyRangeForRestInterface() {
        UnsupportedSmartcardRangeException exception = assertThrows(
                UnsupportedSmartcardRangeException.class,
                () -> classifier.validateRestSupported("1092026368")
        );

        assertTrue(exception.getMessage().contains("legacy SOA/SMS"));
    }

    @Test
    void shouldRejectUnsupportedRangeForRestInterface() {
        UnsupportedSmartcardRangeException exception = assertThrows(
                UnsupportedSmartcardRangeException.class,
                () -> classifier.validateRestSupported("9999999999")
        );

        assertTrue(exception.getMessage().contains("not supported"));
    }

    @Test
    void shouldRejectInvalidUaFormat() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> classifier.classify("ABC")
        );

        assertTrue(exception.getMessage().contains("digits"));
    }
}