-- Student-to-student exchange requests (no admin approval needed for individual requests).
CREATE TABLE exchange_requests (
    request_id                      BIGINT        NOT NULL AUTO_INCREMENT,
    item_id                         BIGINT        NOT NULL,
    requester_id                    BIGINT        NOT NULL,
    offered_item_id                 BIGINT        NULL,     -- required for SWAP, NULL for GIVEAWAY / UPCYCLING
    message                         VARCHAR(500)  NULL,
    status                          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    status_reason                   VARCHAR(500)  NULL,     -- optional note when rejected / cancelled / closed
    owner_confirmed_completion      BOOLEAN       NOT NULL DEFAULT FALSE,
    requester_confirmed_completion  BOOLEAN       NOT NULL DEFAULT FALSE,
    completed_at                    DATETIME(6)   NULL,
    created_at                      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    -- One active (PENDING / ACCEPTED) request per requester and item. MySQL has no partial
    -- unique indexes, so a generated key that is NULL for inactive requests is used instead.
    active_request_key VARCHAR(50) GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'ACCEPTED') THEN CONCAT(item_id, ':', requester_id) END
    ) STORED,
    PRIMARY KEY (request_id),
    CONSTRAINT fk_requests_item      FOREIGN KEY (item_id)         REFERENCES items (item_id),
    CONSTRAINT fk_requests_requester FOREIGN KEY (requester_id)    REFERENCES users (user_id),
    CONSTRAINT fk_requests_offered   FOREIGN KEY (offered_item_id) REFERENCES items (item_id),
    CONSTRAINT uk_requests_active UNIQUE (active_request_key),
    CONSTRAINT ck_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'CLOSED', 'COMPLETED')),
    CONSTRAINT ck_requests_not_same_item CHECK (offered_item_id IS NULL OR offered_item_id <> item_id),
    INDEX idx_requests_item_status (item_id, status),
    INDEX idx_requests_requester (requester_id, status, created_at),
    INDEX idx_requests_offered (offered_item_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
