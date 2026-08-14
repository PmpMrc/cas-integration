package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.AdmAccountRequest;
import it.tivusat.cas.infrastructure.nagra.dto.AdmDeviceRequest;
import it.tivusat.cas.infrastructure.nagra.dto.NagraDeviceResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class NagraAdmClient {

    private final NagraProperties properties;
    private final NagraRestExecutor executor;

    public NagraAdmClient(
            NagraProperties properties,
            NagraRestExecutor executor
    ) {
        this.properties = properties;
        this.executor = executor;
    }

    public void createAccount(SmartcardSource source, AdmAccountRequest request) {
        executor.execute(
                NagraOperation.ADM_CREATE_ACCOUNT,
                request.id(),
                HttpMethod.POST,
                properties.paths().createAccount(),
                source,
                "W",
                request
        );
    }

    public void createDevice(SmartcardSource source, AdmDeviceRequest request) {
        executor.execute(
                NagraOperation.ADM_CREATE_DEVICE,
                request.id(),
                HttpMethod.POST,
                properties.paths().createDevice(),
                source,
                "N",
                request
        );
    }

    public void updateDeviceForRefresh(
            SmartcardSource source,
            String sn,
            AdmDeviceRequest request
    ) {
        executor.execute(
                NagraOperation.ADM_REFRESH_DEVICE,
                sn,
                HttpMethod.PUT,
                properties.paths().updateDevice(),
                source,
                "1",
                request,
                sn
        );
    }

    public void suspendDevice(SmartcardSource source, String sn) {
        executor.execute(
                NagraOperation.ADM_SUSPEND_DEVICE,
                sn,
                HttpMethod.PUT,
                properties.paths().updateDevice(),
                source,
                "1",
                AdmDeviceRequest.suspend(),
                sn
        );
    }

    public void deleteDevice(SmartcardSource source, String sn) {
        executor.execute(
                NagraOperation.ADM_DELETE_DEVICE,
                sn,
                HttpMethod.DELETE,
                properties.paths().updateDevice() + "?cancelICC=true",
                source,
                "1",
                null,
                sn
        );
    }

    public NagraDeviceResponse getDevice(SmartcardSource source, String sn) {
        return executor.execute(
                NagraOperation.ADM_GET_DEVICE,
                sn,
                HttpMethod.GET,
                properties.paths().getDevice(),
                source,
                "W",
                null,
                NagraDeviceResponse.class,
                sn
        );
    }
}