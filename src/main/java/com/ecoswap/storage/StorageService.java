package com.ecoswap.storage;

import org.springframework.core.io.Resource;

/**
 * File storage abstraction. The local implementation writes to disk outside the web root;
 * an S3 / Cloudflare R2 implementation can be added behind the same interface without
 * touching any service that stores files.
 */
public interface StorageService {

    /** Stores validated bytes under a random name and returns its key. */
    StoredFile store(StorageFolder folder, byte[] content, DetectedFileType type, String originalFilename);

    /** Loads a file by key; throws NotFoundException if it does not exist. */
    Resource load(String key);

    /** Deletes a file if it exists. Never throws for a missing file. */
    void delete(String key);
}
