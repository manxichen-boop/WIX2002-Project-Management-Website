package com.ecoswap.config;

import com.ecoswap.storage.DatabaseStorageService;
import com.ecoswap.storage.LocalStorageService;
import com.ecoswap.storage.StorageService;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Selects the storage implementation with ECOSWAP_STORAGE_TYPE:
 * "local" (default, files under ECOSWAP_STORAGE_DIR) or "database" (files in MySQL, for hosts
 * without a persistent disk). An S3 / R2 implementation could be added the same way.
 */
@Configuration
public class StorageConfig {

    @Bean
    StorageService storageService(AppProperties props, JdbcTemplate jdbc,
                                  @Value("${ecoswap.storage.type:local}") String type) {
        if ("database".equalsIgnoreCase(type.strip())) {
            return new DatabaseStorageService(jdbc);
        }
        return new LocalStorageService(Path.of(props.storage().localDir()));
    }
}
