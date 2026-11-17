package com.ecoswap.storage;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Keeps disk and database consistent: files written during a transaction are removed if it
 * rolls back, and files scheduled for deletion are only removed once it commits.
 */
@Component
public class FileCleanup {

    private final StorageService storage;

    public FileCleanup(StorageService storage) {
        this.storage = storage;
    }

    /** The list is read when the transaction ends, so callers may keep adding keys after registering it. */
    public void deleteOnRollback(List<String> keys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    keys.forEach(storage::delete);
                }
            }
        });
    }

    public void deleteAfterCommit(List<String> keys) {
        List<String> copy = new ArrayList<>(keys);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                copy.forEach(storage::delete);
            }
        });
    }
}
