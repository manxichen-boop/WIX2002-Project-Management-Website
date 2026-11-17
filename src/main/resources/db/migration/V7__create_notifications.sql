CREATE TABLE notifications (
    notification_id    BIGINT        NOT NULL AUTO_INCREMENT,
    user_id            BIGINT        NOT NULL,
    title              VARCHAR(150)  NOT NULL,
    message            VARCHAR(500)  NOT NULL,
    notification_type  VARCHAR(40)   NOT NULL,
    link_url           VARCHAR(255)  NULL,      -- in-app page the notification points to
    is_read            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    INDEX idx_notifications_user (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
