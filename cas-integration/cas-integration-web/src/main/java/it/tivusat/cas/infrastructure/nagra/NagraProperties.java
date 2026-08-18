package it.tivusat.cas.infrastructure.nagra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nagra")
public class NagraProperties {

    private boolean enabled;
    private String baseUrl;
    private SourceId sourceId = new SourceId();
    private Paths paths = new Paths();

    public boolean isEnabled() {
        return enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public SourceId getSourceId() {
        return sourceId;
    }

    public SourceId sourceId() {
        return sourceId;
    }

    public void setSourceId(SourceId sourceId) {
        this.sourceId = sourceId;
    }

    public Paths getPaths() {
        return paths;
    }

    public Paths paths() {
        return paths;
    }

    public void setPaths(Paths paths) {
        this.paths = paths;
    }

    public static class SourceId {

        private String physical;
        private String virtual;

        public String getPhysical() {
            return physical;
        }

        public String physical() {
            return physical;
        }

        public void setPhysical(String physical) {
            this.physical = physical;
        }

        public String getVirtual() {
            return virtual;
        }

        public String virtual() {
            return virtual;
        }

        public void setVirtual(String virtualValue) {
            this.virtual = virtualValue;
        }
    }

    public static class Paths {

        private String createAccount;
        private String createEntitlement;
        private String createDevice;
        private String updateDevice;
        private String getDevice;
        private String getEntitlements;

        public String getCreateAccount() {
            return createAccount;
        }

        public String createAccount() {
            return createAccount;
        }

        public void setCreateAccount(String createAccount) {
            this.createAccount = createAccount;
        }

        public String getCreateEntitlement() {
            return createEntitlement;
        }

        public String createEntitlement() {
            return createEntitlement;
        }

        public void setCreateEntitlement(String createEntitlement) {
            this.createEntitlement = createEntitlement;
        }

        public String getCreateDevice() {
            return createDevice;
        }

        public String createDevice() {
            return createDevice;
        }

        public void setCreateDevice(String createDevice) {
            this.createDevice = createDevice;
        }

        public String getUpdateDevice() {
            return updateDevice;
        }

        public String updateDevice() {
            return updateDevice;
        }

        public void setUpdateDevice(String updateDevice) {
            this.updateDevice = updateDevice;
        }

        public String getGetDevice() {
            return getDevice;
        }

        public String getDevice() {
            return getDevice;
        }

        public void setGetDevice(String getDevice) {
            this.getDevice = getDevice;
        }

        public String getGetEntitlements() {
            return getEntitlements;
        }

        public String getEntitlements() {
            return getEntitlements;
        }

        public void setGetEntitlements(String getEntitlements) {
            this.getEntitlements = getEntitlements;
        }
    }
}