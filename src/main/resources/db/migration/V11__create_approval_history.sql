-- Every moderation decision on a registration or a listing, in order.
CREATE TABLE approval_history (
    approval_id  BIGINT        NOT NULL AUTO_INCREMENT,
    entity_type  VARCHAR(20)   NOT NULL,
    entity_id    BIGINT        NOT NULL,
    admin_id     BIGINT        NOT NULL,
    decision     VARCHAR(20)   NOT NULL,
    reason       VARCHAR(500)  NULL,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (approval_id),
    CONSTRAINT fk_approval_admin FOREIGN KEY (admin_id) REFERENCES users (user_id),
    CONSTRAINT ck_approval_entity CHECK (entity_type IN ('USER', 'ITEM')),
    CONSTRAINT ck_approval_decision CHECK (decision IN ('APPROVED', 'REJECTED', 'NEEDS_CHANGES', 'SUSPENDED', 'REACTIVATED', 'ARCHIVED')),
    INDEX idx_approval_entity (entity_type, entity_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
