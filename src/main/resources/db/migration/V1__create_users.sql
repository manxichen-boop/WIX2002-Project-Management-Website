-- Student and administrator accounts. Exactly two roles exist: USER and ADMIN.
-- Account approval (account_status) is deliberately separate from anything item-related.
CREATE TABLE users (
    user_id             BIGINT        NOT NULL AUTO_INCREMENT,
    full_name           VARCHAR(120)  NOT NULL,
    email               VARCHAR(190)  NOT NULL,
    password_hash       VARCHAR(100)  NOT NULL,
    profile_image_path  VARCHAR(255)  NULL,
    university          VARCHAR(150)  NOT NULL,
    role                VARCHAR(10)   NOT NULL DEFAULT 'USER',
    account_status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING_APPROVAL',
    status_reason       VARCHAR(500)  NULL,     -- latest rejection / suspension reason shown to the student
    email_verified      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (account_status IN ('PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'SUSPENDED')),
    INDEX idx_users_status_created (account_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
