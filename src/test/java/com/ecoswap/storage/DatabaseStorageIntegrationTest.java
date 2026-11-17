package com.ecoswap.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/** ECOSWAP_STORAGE_TYPE=database keeps photos in MySQL (for hosts without a persistent disk). */
class DatabaseStorageIntegrationTest extends IntegrationTestBase {

    @Test
    void storesLoadsAndDeletesFilesInMysql() throws Exception {
        DatabaseStorageService storage = new DatabaseStorageService(jdbc);
        byte[] bytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
        StoredFile f = storage.store(StorageFolder.values()[0], bytes, DetectedFileType.values()[0], "photo.jpg");

        assertThat(f.key()).matches("[a-z]+/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.[a-z]+");
        assertThat(storage.load(f.key()).getContentAsByteArray()).isEqualTo(bytes);
        assertThatThrownBy(() -> storage.load("../etc/passwd")).isInstanceOf(NotFoundException.class);

        storage.delete(f.key());
        storage.delete(f.key());   // deleting twice is harmless
        assertThatThrownBy(() -> storage.load(f.key())).isInstanceOf(NotFoundException.class);
    }
}
