-- Private chat between the two students of an exchange request (opens once the request is accepted).
CREATE TABLE chat_messages (
    message_id   BIGINT        NOT NULL AUTO_INCREMENT,
    request_id   BIGINT        NOT NULL,
    sender_id    BIGINT        NOT NULL,
    body         VARCHAR(2000) NOT NULL,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at      DATETIME(6)   NULL,      -- when the other participant first saw it
    PRIMARY KEY (message_id),
    CONSTRAINT fk_chat_request FOREIGN KEY (request_id) REFERENCES exchange_requests (request_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sender  FOREIGN KEY (sender_id)  REFERENCES users (user_id),
    INDEX idx_chat_thread (request_id, message_id),
    INDEX idx_chat_unread (request_id, sender_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
