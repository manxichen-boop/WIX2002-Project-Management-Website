package com.ecoswap.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.config.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** Upload validation by file content, file-name sanitising, random storage names and path-traversal protection. */
class StorageAndUploadTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};

    private final UploadValidator validator = new UploadValidator(new AppProperties("http://localhost", "UTC",
            new AppProperties.Storage("./storage", 1024, 6, 1024), null, null, null));

    @Test
    void detectsTypesFromContentNotNames() {
        assertThat(DetectedFileType.detect(JPEG)).isEqualTo(DetectedFileType.JPEG);
        assertThat(DetectedFileType.detect(PNG)).isEqualTo(DetectedFileType.PNG);
        assertThat(DetectedFileType.detect(WEBP)).isEqualTo(DetectedFileType.WEBP);
        assertThat(DetectedFileType.detect(PDF)).isEqualTo(DetectedFileType.PDF);
        assertThat(DetectedFileType.detect("<?php echo 1; ?>".getBytes())).isNull();
    }

    @Test
    void acceptsRealImagesWithMatchingExtension() {
        var upload = validator.image(new MockMultipartFile("photos", "Cover.JPG", "image/jpeg", JPEG), "photos");
        assertThat(upload.type()).isEqualTo(DetectedFileType.JPEG);
    }

    @Test
    void rejectsSpoofedTypesWrongExtensionsAndOversizedFiles() {
        // HTML disguised as a JPEG (browser content-type is not trusted)
        assertThatThrownBy(() -> validator.image(new MockMultipartFile("p", "x.jpg", "image/jpeg", "<html>".getBytes()), "photos"))
                .isInstanceOf(InvalidInputException.class);
        // real PNG bytes but an executable extension
        assertThatThrownBy(() -> validator.image(new MockMultipartFile("p", "x.exe", "image/png", PNG), "photos"))
                .isInstanceOf(InvalidInputException.class);
        // PDFs are allowed for support attachments only
        assertThatThrownBy(() -> validator.image(new MockMultipartFile("p", "x.pdf", "application/pdf", PDF), "photos"))
                .isInstanceOf(InvalidInputException.class);
        assertThat(validator.attachment(new MockMultipartFile("a", "x.pdf", "application/pdf", PDF), "attachment").type())
                .isEqualTo(DetectedFileType.PDF);
        assertThatThrownBy(() -> validator.image(new MockMultipartFile("p", "big.jpg", "image/jpeg", new byte[2048]), "photos"))
                .hasMessageContaining("MB or smaller");
    }

    @Test
    void sanitisesClientFileNames() {
        assertThat(UploadValidator.sanitiseName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(UploadValidator.sanitiseName("C:\\Users\\me\\photo<1>.jpg")).isEqualTo("photo_1_.jpg");
        assertThat(UploadValidator.sanitiseName(null)).isEqualTo("upload");
    }

    @Test
    void storesWithRandomNamesAndBlocksPathTraversal(@TempDir Path root) throws Exception {
        LocalStorageService storage = new LocalStorageService(root);
        StoredFile a = storage.store(StorageFolder.ITEMS, JPEG, DetectedFileType.JPEG, "same.jpg");
        StoredFile b = storage.store(StorageFolder.ITEMS, JPEG, DetectedFileType.JPEG, "same.jpg");
        assertThat(a.key()).startsWith("items/").endsWith(".jpg").isNotEqualTo(b.key());
        assertThat(Files.readAllBytes(storage.load(a.key()).getFile().toPath())).isEqualTo(JPEG);

        Files.writeString(root.resolve("secret.txt"), "top secret");
        assertThatThrownBy(() -> storage.load("items/../secret.txt")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> storage.load("../outside.txt")).isInstanceOf(NotFoundException.class);

        storage.delete(a.key());
        assertThatThrownBy(() -> storage.load(a.key())).isInstanceOf(NotFoundException.class);
    }
}
