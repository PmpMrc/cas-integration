package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubscriptionEntitlementFactoryTest {

    private static final String SN = "109687603246";

    @Test
    void shouldBuildSubscriptionWithTypedAccountAndWholeSecondDates() {
        RmgEntitlementRequest request =
                SubscriptionEntitlementFactory.create(
                        SN,
                        SmartcardType.TIVU_HD,
                        Instant.parse("2026-10-29T17:03:16.420Z")
                );

        assertEquals(SN + "_TivuHD", request.id());
        assertEquals(SN + "_TivuHD", request.accountId());
        assertEquals("22", request.productId());
        assertEquals("SUBSCRIBED", request.status());
        assertEquals("ABSOLUTE", request.validityType());
        assertEquals("SUBSCRIPTION", request.productType());

        assertEquals(
                Instant.parse("2026-10-29T17:03:16Z"),
                request.validFrom()
        );
        assertEquals(
                Instant.parse("2030-10-29T17:03:16Z"),
                request.expiryDate()
        );
    }

    @Test
    void shouldUseFourCalendarYearsIncludingLeapYears() {
        RmgEntitlementRequest request =
                SubscriptionEntitlementFactory.create(
                        SN,
                        SmartcardType.TIVU_HD,
                        Instant.parse("2028-02-29T12:00:00Z")
                );

        assertEquals(
                Instant.parse("2032-02-29T12:00:00Z"),
                request.expiryDate()
        );
    }

    @Test
    void shouldRejectMissingType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SubscriptionEntitlementFactory.create(
                        SN,
                        null,
                        Instant.parse("2026-10-29T17:03:16Z")
                )
        );
    }
}