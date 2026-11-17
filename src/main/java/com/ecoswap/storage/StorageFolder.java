package com.ecoswap.storage;

/** Top-level storage areas (Part 5). */
public enum StorageFolder {
    ITEMS("items"), PROFILES("profiles"), SUPPORT("support"), TEMPORARY("temporary");

    private final String path;

    StorageFolder(String path) { this.path = path; }

    public String path() { return path; }
}
