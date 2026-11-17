-- Item listings. Moderation (approval_status) and availability (availability_status)
-- are separate fields on purpose: an APPROVED item can be AVAILABLE, RESERVED or COMPLETED.
-- "condition" is a reserved word in MySQL, so the column is named item_condition.
CREATE TABLE items (
    item_id              BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id             BIGINT        NOT NULL,
    category_id          BIGINT        NOT NULL,
    item_name            VARCHAR(120)  NOT NULL,
    description          VARCHAR(2000) NOT NULL,
    item_condition       VARCHAR(20)   NOT NULL,
    listing_type         VARCHAR(20)   NOT NULL,
    pickup_location      VARCHAR(150)  NOT NULL,
    approximate_weight   DECIMAL(8,2)  NULL,      -- kilograms, optional, owner-reported
    approval_status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING_APPROVAL',
    availability_status  VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
    reviewed_by          BIGINT        NULL,
    reviewed_at          DATETIME(6)   NULL,
    rejection_reason     VARCHAR(500)  NULL,      -- reason for REJECTED / NEEDS_CHANGES / SUSPENDED
    version              BIGINT        NOT NULL DEFAULT 0,  -- optimistic locking for concurrent edits
    created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (item_id),
    CONSTRAINT fk_items_owner    FOREIGN KEY (owner_id)    REFERENCES users (user_id),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories (category_id),
    CONSTRAINT fk_items_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (user_id),
    CONSTRAINT ck_items_condition CHECK (item_condition IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'WORN')),
    CONSTRAINT ck_items_type CHECK (listing_type IN ('SWAP', 'GIVEAWAY', 'UPCYCLING')),
    CONSTRAINT ck_items_approval CHECK (approval_status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'NEEDS_CHANGES', 'SUSPENDED')),
    CONSTRAINT ck_items_availability CHECK (availability_status IN ('AVAILABLE', 'RESERVED', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT ck_items_weight CHECK (approximate_weight IS NULL OR (approximate_weight > 0 AND approximate_weight <= 500)),
    INDEX idx_items_public (approval_status, availability_status, created_at),
    INDEX idx_items_owner (owner_id, created_at),
    INDEX idx_items_category (category_id),
    INDEX idx_items_type (listing_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
