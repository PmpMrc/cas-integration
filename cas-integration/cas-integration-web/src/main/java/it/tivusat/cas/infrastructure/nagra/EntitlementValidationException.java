package it.tivusat.cas.infrastructure.nagra;

import java.util.LinkedHashMap;
import java.util.Map;

/** Describes why an activation was stopped before creating the ADM device. */
public final class EntitlementValidationException extends IllegalStateException {

    private final String reason;
    private final String smartcardSn;
    private final String actualAccountId;
    private final String actualStatus;
    private final String entitlementId;

    private EntitlementValidationException(
            String reason,
            String smartcardSn,
            String actualAccountId,
            String actualStatus,
            String entitlementId,
            String message
    ) {
        super(message);
        this.reason = reason;
        this.smartcardSn = smartcardSn;
        this.actualAccountId = actualAccountId;
        this.actualStatus = actualStatus;
        this.entitlementId = entitlementId;
    }

    public static EntitlementValidationException notFound(String sn) {
        return new EntitlementValidationException(
                "NOT_FOUND", sn, null, null, null,
                "Cannot activate smartcard " + sn
                        + ": RMG returned no entitlement for this account. ADM device was not created."
        );
    }

    public static EntitlementValidationException missingAccountId(String sn, String id, String status) {
        return new EntitlementValidationException(
                "MISSING_ACCOUNT_ID", sn, null, status, id,
                "Cannot activate smartcard " + sn
                        + ": RMG returned an entitlement without accountId. ADM device was not created."
        );
    }

    public static EntitlementValidationException accountMismatch(
            String sn, String accountId, String status, String id
    ) {
        return new EntitlementValidationException(
                "ACCOUNT_MISMATCH", sn, accountId, status, id,
                "Cannot activate smartcard " + sn + ": RMG returned accountId " + accountId
                        + " instead of " + sn + ". ADM device was not created."
        );
    }

    public static EntitlementValidationException missingStatus(String sn, String id) {
        return new EntitlementValidationException(
                "MISSING_STATUS", sn, sn, null, id,
                "Cannot activate smartcard " + sn
                        + ": RMG returned an entitlement without status. Expected SUBSCRIBED; ADM device was not created."
        );
    }

    public static EntitlementValidationException statusMismatch(String sn, String status, String id) {
        return new EntitlementValidationException(
                "STATUS_NOT_SUBSCRIBED", sn, sn, status, id,
                "Cannot activate smartcard " + sn + ": RMG entitlement status is " + status
                        + " instead of SUBSCRIBED. ADM device was not created."
        );
    }

    public Map<String, String> details() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("operation", "RMG_GET_ENTITLEMENTS");
        details.put("reason", reason);
        details.put("smartcardSn", smartcardSn);
        details.put("expectedAccountId", smartcardSn);
        details.put("expectedStatus", "SUBSCRIBED");
        if (actualAccountId != null) {
            details.put("actualAccountId", actualAccountId);
        }
        if (actualStatus != null) {
            details.put("actualStatus", actualStatus);
        }
        if (entitlementId != null) {
            details.put("entitlementId", entitlementId);
        }
        return details;
    }
}
