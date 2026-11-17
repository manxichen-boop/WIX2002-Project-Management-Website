CREATE TABLE support_replies (
    reply_id    BIGINT         NOT NULL AUTO_INCREMENT,
    ticket_id   BIGINT         NOT NULL,
    sender_id   BIGINT         NOT NULL,
    message     VARCHAR(4000)  NOT NULL,
    created_at  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (reply_id),
    CONSTRAINT fk_replies_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (ticket_id) ON DELETE CASCADE,
    CONSTRAINT fk_replies_sender FOREIGN KEY (sender_id) REFERENCES users (user_id),
    INDEX idx_replies_ticket (ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
