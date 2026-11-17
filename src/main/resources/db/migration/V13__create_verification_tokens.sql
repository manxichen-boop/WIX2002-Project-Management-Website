-- Only a SHA-256 hash of each token is stored; the raw token exists only in the e-mailed link.
CREATE TABLE email_verification_tokens (
    token_id     BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    token_hash   CHAR(64)     NOT NULL,
    expiry_date  DATETIME(6)  NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (token_id),
    CONSTRAINT uk_email_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_token_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    INDEX idx_email_token_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
