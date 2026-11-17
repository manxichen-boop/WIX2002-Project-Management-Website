package com.ecoswap.storage;

/** Result of storing a file: the storage key (what MySQL keeps) plus metadata. */
public record StoredFile(String key, String mimeType, long size, String originalFilename) {}
