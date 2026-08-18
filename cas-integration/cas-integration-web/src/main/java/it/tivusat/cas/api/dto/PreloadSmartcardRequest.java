package it.tivusat.cas.api.dto;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PreloadSmartcardRequest {

        @NotBlank
        private String sn;

        @NotNull
        private SmartcardType smartcardType;

        @NotNull
        private SmartcardSource source;

        @NotBlank
        private String productId;

        public PreloadSmartcardRequest() {
        }

        public PreloadSmartcardRequest(String sn, SmartcardType smartcardType, SmartcardSource source, String productId) {
                this.sn = sn;
                this.smartcardType = smartcardType;
                this.source = source;
                this.productId = productId;
        }

        public String getSn() {
                return sn;
        }

        public void setSn(String sn) {
                this.sn = sn;
        }

        public SmartcardType getSmartcardType() {
                return smartcardType;
        }

        public void setSmartcardType(SmartcardType smartcardType) {
                this.smartcardType = smartcardType;
        }

        public SmartcardSource getSource() {
                return source;
        }

        public void setSource(SmartcardSource source) {
                this.source = source;
        }

        public String getProductId() {
                return productId;
        }

        public void setProductId(String productId) {
                this.productId = productId;
        }

        public String sn() {
                return sn;
        }

        public SmartcardType smartcardType() {
                return smartcardType;
        }

        public SmartcardSource source() {
                return source;
        }

        public String productId() {
                return productId;
        }
}