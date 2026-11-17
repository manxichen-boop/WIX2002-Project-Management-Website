package com.ecoswap.storage;

import com.ecoswap.common.exception.NotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.YearMonth;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/** Stores files under ECOSWAP_STORAGE_DIR with random names, e.g. items/2026/11/3f2a….webp. */
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path root;

    public LocalStorageService(Path root) {
        this.root = root.toAbsolutePath().normalize();
        try {
            for (StorageFolder folder : StorageFolder.values()) {
                Files.createDirectories(this.root.resolve(folder.path()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage folders under " + this.root, e);
        }
        log.info("File storage root: {}", this.root);
    }

    @Override
    public StoredFile store(StorageFolder folder, byte[] content, DetectedFileType type, String originalFilename) {
        YearMonth month = YearMonth.now();
        String key = "%s/%d/%02d/%s.%s".formatted(folder.path(), month.getYear(), month.getMonthValue(),
                UUID.randomUUID(), type.extension());
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file", e);
        }
        return new StoredFile(key, type.mimeType(), content.length, originalFilename);
    }

    @Override
    public Resource load(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("File");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete stored file {}: {}", key, e.getMessage());
        }
    }

    /** Resolves a key and refuses anything that would escape the storage root (path traversal). */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || key.contains("..")) {
            throw new NotFoundException("File");
        }
        return path;
    }
}
