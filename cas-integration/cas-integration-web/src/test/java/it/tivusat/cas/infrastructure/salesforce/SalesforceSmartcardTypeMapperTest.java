package it.tivusat.cas.infrastructure.salesforce;

import it.tivusat.cas.domain.SmartcardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SalesforceSmartcardTypeMapperTest {

    private SalesforceSmartcardTypeMapper mapper;

    @BeforeEach
    void setUp() {
        SalesforceProperties properties = new SalesforceProperties();

        Map<String, SmartcardType> mapping = new LinkedHashMap<>();
        mapping.put("CRM_HD_TEST", SmartcardType.TIVU_HD);
        mapping.put("CRM_4K_TEST", SmartcardType.TIVU_4K);
        mapping.put("CRM HD TEST", SmartcardType.TIVU_HD);

        properties.setTypeMapping(mapping);

        mapper = new SalesforceSmartcardTypeMapper(properties);
    }

    @Test
    void shouldMapConfiguredValues() {
        assertEquals(
                SmartcardType.TIVU_HD,
                mapper.map("CRM_HD_TEST")
        );

        assertEquals(
                SmartcardType.TIVU_4K,
                mapper.map("CRM_4K_TEST")
        );
    }

    @Test
    void shouldIgnoreLeadingAndTrailingSpaces() {
        assertEquals(
                SmartcardType.TIVU_HD,
                mapper.map("  CRM_HD_TEST  ")
        );
    }

    @Test
    void shouldPreserveInternalSpaces() {
        assertEquals(
                SmartcardType.TIVU_HD,
                mapper.map("CRM HD TEST")
        );
    }

    @Test
    void shouldRejectMissingType() {
        for (String value : new String[]{null, "", "   "}) {
            SalesforceException error = assertThrows(
                    SalesforceException.class,
                    () -> mapper.map(value)
            );

            assertEquals(
                    SalesforceException.FailureType.SMARTCARD_TYPE_MISSING,
                    error.getFailureType()
            );
        }
    }

    @Test
    void shouldRejectUnknownTypeWithoutDefault() {
        SalesforceException error = assertThrows(
                SalesforceException.class,
                () -> mapper.map("UNKNOWN_TYPE")
        );

        assertEquals(
                SalesforceException.FailureType.UNSUPPORTED_SMARTCARD_TYPE,
                error.getFailureType()
        );
    }

    @Test
    void shouldRequireExactCase() {
        SalesforceException error = assertThrows(
                SalesforceException.class,
                () -> mapper.map("crm_hd_test")
        );

        assertEquals(
                SalesforceException.FailureType.UNSUPPORTED_SMARTCARD_TYPE,
                error.getFailureType()
        );
    }

    @Test
    void shouldNotAutomaticallyAcceptEnumNames() {
        SalesforceException error = assertThrows(
                SalesforceException.class,
                () -> mapper.map("TIVU_HD")
        );

        assertEquals(
                SalesforceException.FailureType.UNSUPPORTED_SMARTCARD_TYPE,
                error.getFailureType()
        );
    }

    @Test
    void shouldBindMappingWithSpacesFromConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        SalesforceConfig.class,
                        SalesforceTokenProvider.class,
                        SalesforceSmartcardTypeMapper.class
                )
                .withPropertyValues(
                        "salesforce.enabled=false",
                        "salesforce.type-mapping[CRM HD TEST]=TIVU_HD"
                )
                .run(context -> {
                    assertNull(context.getStartupFailure());

                    SalesforceSmartcardTypeMapper configuredMapper =
                            context.getBean(
                                    SalesforceSmartcardTypeMapper.class
                            );

                    assertEquals(
                            SmartcardType.TIVU_HD,
                            configuredMapper.map("CRM HD TEST")
                    );
                });
    }
}