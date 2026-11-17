CREATE TABLE support_tickets (
    ticket_id                     BIGINT         NOT NULL AUTO_INCREMENT,
    reference_code                VARCHAR(20)    NOT NULL,   -- shown to the student as the confirmation number
    user_id                       BIGINT         NOT NULL,
    subject                       VARCHAR(150)   NOT NULL,
    message                       VARCHAR(4000)  NOT NULL,
    attachment_path               VARCHAR(255)   NULL,       -- storage key, never a public path
    attachment_original_filename  VARCHAR(255)   NULL,
    attachment_mime_type          VARCHAR(50)    NULL,
    status                        VARCHAR(20)    NOT NULL DEFAULT 'OPEN',
    created_at                    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (ticket_id),
    CONSTRAINT uk_support_reference UNIQUE (reference_code),
    CONSTRAINT fk_support_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_support_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_USER', 'RESOLVED', 'CLOSED')),
    INDEX idx_support_user (user_id, created_at),
    INDEX idx_support_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
