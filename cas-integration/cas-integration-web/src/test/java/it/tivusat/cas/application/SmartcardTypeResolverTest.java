package it.tivusat.cas.application;

import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.salesforce.SalesforceException;
import it.tivusat.cas.infrastructure.salesforce.SalesforceSmartcardClient;
import it.tivusat.cas.infrastructure.salesforce.SalesforceSmartcardTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartcardTypeResolverTest {

    private static final String SN = "109687603246";

    @Mock
    private SalesforceSmartcardClient salesforceClient;

    @Mock
    private SalesforceSmartcardTypeMapper typeMapper;

    private SmartcardTypeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SmartcardTypeResolver(
                salesforceClient,
                typeMapper
        );
    }

    @Test
    void shouldUseProvidedTypeWithoutCallingSalesforce() {
        SmartcardType result =
                resolver.resolve(SN, SmartcardType.TIVU_HD);

        assertEquals(SmartcardType.TIVU_HD, result);

        verifyNoInteractions(salesforceClient, typeMapper);
    }

    @Test
    void shouldLookupAndMapTypeWhenMissing() {
        when(salesforceClient.findSmartcardType(SN))
                .thenReturn("CRM_HD_TEST");

        when(typeMapper.map("CRM_HD_TEST"))
                .thenReturn(SmartcardType.TIVU_HD);

        SmartcardType result = resolver.resolve(SN, null);

        assertEquals(SmartcardType.TIVU_HD, result);

        verify(salesforceClient).findSmartcardType(SN);
        verify(typeMapper).map("CRM_HD_TEST");
        verifyNoMoreInteractions(salesforceClient, typeMapper);
    }

    @Test
    void shouldPropagateLookupFailureWithoutMapping() {
        SalesforceException failure = new SalesforceException(
                SalesforceException.FailureType.SMARTCARD_NOT_FOUND,
                "Smartcard was not found in Salesforce",
                null
        );

        when(salesforceClient.findSmartcardType(SN))
                .thenThrow(failure);

        SalesforceException actual = assertThrows(
                SalesforceException.class,
                () -> resolver.resolve(SN, null)
        );

        assertSame(failure, actual);
        verifyNoInteractions(typeMapper);
    }

    @Test
    void shouldPropagateMappingFailureWithoutDefault() {
        when(salesforceClient.findSmartcardType(SN))
                .thenReturn("UNKNOWN_TYPE");

        SalesforceException failure = new SalesforceException(
                SalesforceException.FailureType.UNSUPPORTED_SMARTCARD_TYPE,
                "Salesforce smartcard type has no configured mapping",
                null
        );

        when(typeMapper.map("UNKNOWN_TYPE"))
                .thenThrow(failure);

        SalesforceException actual = assertThrows(
                SalesforceException.class,
                () -> resolver.resolve(SN, null)
        );

        assertSame(failure, actual);
    }

    @Test
    void shouldPropagateDisabledIntegrationWhenTypeIsMissing() {
        SalesforceException failure = new SalesforceException(
                SalesforceException.FailureType.INTEGRATION_DISABLED,
                "Salesforce integration is disabled",
                null
        );

        when(salesforceClient.findSmartcardType(SN))
                .thenThrow(failure);

        SalesforceException actual = assertThrows(
                SalesforceException.class,
                () -> resolver.resolve(SN, null)
        );

        assertSame(failure, actual);
        verifyNoInteractions(typeMapper);
    }
}