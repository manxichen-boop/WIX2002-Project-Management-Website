-- Append-only record of administrative actions. The application never updates or deletes rows here.
CREATE TABLE admin_audit_logs (
    log_id       BIGINT        NOT NULL AUTO_INCREMENT,
    admin_id     BIGINT        NOT NULL,
    action_type  VARCHAR(40)   NOT NULL,
    target_type  VARCHAR(30)   NOT NULL,
    target_id    BIGINT        NOT NULL,
    reason       VARCHAR(1000) NULL,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (log_id),
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id) REFERENCES users (user_id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_action (action_type, created_at),
    INDEX idx_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
