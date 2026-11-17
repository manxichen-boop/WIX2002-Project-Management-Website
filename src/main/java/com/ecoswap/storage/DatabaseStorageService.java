package com.ecoswap.storage;

import com.ecoswap.common.exception.NotFoundException;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Keeps uploaded files in MySQL (table stored_files). Meant for free hosting plans whose disk is
 * wiped on every restart; the local-disk storage is still the better choice when a volume exists.
 * Runs inside the caller's transaction, so a failed listing never leaves an orphan photo behind.
 */
public class DatabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStorageService.class);

    private final JdbcTemplate jdbc;

    public DatabaseStorageService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        log.info("File storage: database table stored_files");
    }

    @Override
    public StoredFile store(StorageFolder folder, byte[] content, DetectedFileType type, String originalFilename) {
        YearMonth month = YearMonth.now();
        String key = "%s/%d/%02d/%s.%s".formatted(folder.path(), month.getYear(), month.getMonthValue(),
                UUID.randomUUID(), type.extension());
        jdbc.update("INSERT INTO stored_files (storage_key, content, mime_type, size_bytes) VALUES (?, ?, ?, ?)",
                key, content, type.mimeType(), content.length);
        return new StoredFile(key, type.mimeType(), content.length, originalFilename);
    }

    @Override
    public Resource load(String key) {
        if (key == null || key.contains("..")) {
            throw new NotFoundException("File");
        }
        List<byte[]> rows = jdbc.query("SELECT content FROM stored_files WHERE storage_key = ?",
                (rs, i) -> rs.getBytes(1), key);
        if (rows.isEmpty()) {
            throw new NotFoundException("File");
        }
        String name = key.substring(key.lastIndexOf('/') + 1);
        return new ByteArrayResource(rows.get(0)) {
            @Override
            public String getFilename() { return name; }
        };
    }

    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }
        try {
            jdbc.update("DELETE FROM stored_files WHERE storage_key = ?", key);
        } catch (RuntimeException e) {
            log.warn("Could not delete stored file {}: {}", key, e.getMessage());
        }
    }
}
