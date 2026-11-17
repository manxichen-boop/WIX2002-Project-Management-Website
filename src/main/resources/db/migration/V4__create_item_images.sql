-- Image metadata only. The image bytes live in the file storage (local disk or object storage);
-- image_path is the storage key, never a public filesystem path.
CREATE TABLE item_images (
    image_id           BIGINT        NOT NULL AUTO_INCREMENT,
    item_id            BIGINT        NOT NULL,
    image_path         VARCHAR(255)  NOT NULL,
    original_filename  VARCHAR(255)  NULL,
    mime_type          VARCHAR(50)   NOT NULL,
    file_size          BIGINT        NOT NULL,
    display_order      INT           NOT NULL DEFAULT 0,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (image_id),
    CONSTRAINT fk_item_images_item FOREIGN KEY (item_id) REFERENCES items (item_id) ON DELETE CASCADE,
    CONSTRAINT uk_item_images_path UNIQUE (image_path),
    INDEX idx_item_images_item (item_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
