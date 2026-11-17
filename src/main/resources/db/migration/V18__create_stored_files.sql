-- Optional database-backed file storage (ECOSWAP_STORAGE_TYPE=database), for hosts without a
-- persistent disk. Keys have the same format as the local-disk storage.
CREATE TABLE stored_files (
    storage_key  VARCHAR(200) NOT NULL,
    content      LONGBLOB     NOT NULL,
    mime_type    VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (storage_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
