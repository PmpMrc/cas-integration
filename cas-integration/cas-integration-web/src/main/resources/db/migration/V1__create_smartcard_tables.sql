CREATE TABLE smartcard (
    sn VARCHAR(12) PRIMARY KEY,
    ua VARCHAR(10) NOT NULL,
    smartcard_type VARCHAR(50) NOT NULL,
    source VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    account_created BOOLEAN NOT NULL DEFAULT FALSE,
    device_created BOOLEAN NOT NULL DEFAULT FALSE,
    entitlement_id VARCHAR(100),
    product_id VARCHAR(100),
    ca_sn VARCHAR(50),
    expiry_date TIMESTAMP WITH TIME ZONE,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE nagra_operation_log (
    id UUID PRIMARY KEY,
    smartcard_sn VARCHAR(12),
    operation VARCHAR(50) NOT NULL,
    request_payload TEXT,
    response_payload TEXT,
    http_status INTEGER,
    result VARCHAR(30) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);