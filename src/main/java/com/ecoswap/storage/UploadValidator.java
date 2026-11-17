package com.ecoswap.storage;

import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploads before anything is written: extension allow-list, real type from the
 * file's bytes (the browser-supplied Content-Type and file name are not trusted), and size.
 */
@Component
public class UploadValidator {

    public record ValidatedUpload(byte[] content, DetectedFileType type, String originalFilename) {}

    private static final Set<DetectedFileType> IMAGES = Set.of(DetectedFileType.JPEG, DetectedFileType.PNG, DetectedFileType.WEBP);
    private static final Set<DetectedFileType> IMAGES_AND_PDF = Set.of(DetectedFileType.JPEG, DetectedFileType.PNG,
            DetectedFileType.WEBP, DetectedFileType.PDF);

    private final AppProperties props;

    public UploadValidator(AppProperties props) {
        this.props = props;
    }

    public ValidatedUpload image(MultipartFile file, String field) {
        return validate(file, field, IMAGES, props.storage().maxImageBytes(), "Photos must be JPG, PNG or WebP images");
    }

    public ValidatedUpload attachment(MultipartFile file, String field) {
        return validate(file, field, IMAGES_AND_PDF, props.storage().maxAttachmentBytes(), "Attachments must be a JPG, PNG, WebP image or a PDF");
    }

    private ValidatedUpload validate(MultipartFile file, String field, Set<DetectedFileType> allowed, long maxBytes, String typeMessage) {
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException(field, "The file is empty.");
        }
        if (file.getSize() > maxBytes) {
            throw new InvalidInputException(field, "Each file must be " + (maxBytes / 1024 / 1024) + " MB or smaller.");
        }
        String original = sanitiseName(file.getOriginalFilename());
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read upload", e);
        }
        DetectedFileType type = DetectedFileType.detect(content);
        if (type == null || !allowed.contains(type) || !type.acceptsExtension(ext)) {
            throw new InvalidInputException(field, typeMessage + " (\"" + original + "\" is not).");
        }
        return new ValidatedUpload(content, type, original);
    }

    /** Keeps only a display-safe base name; it is stored as metadata, never used as a path. */
    static String sanitiseName(String name) {
        if (name == null || name.isBlank()) {
            return "upload";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N}._ -]", "_").strip();
        return base.length() > 200 ? base.substring(base.length() - 200) : (base.isEmpty() ? "upload" : base);
    }
}
