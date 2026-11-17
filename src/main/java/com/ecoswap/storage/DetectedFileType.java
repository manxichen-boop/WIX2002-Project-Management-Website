package com.ecoswap.storage;

import java.util.Set;

/** File types accepted by Eco Swape, identified from the file's leading bytes ("magic numbers"). */
public enum DetectedFileType {
    JPEG("image/jpeg", "jpg", Set.of("jpg", "jpeg")),
    PNG("image/png", "png", Set.of("png")),
    WEBP("image/webp", "webp", Set.of("webp")),
    PDF("application/pdf", "pdf", Set.of("pdf"));

    private final String mimeType;
    private final String extension;
    private final Set<String> acceptedExtensions;

    DetectedFileType(String mimeType, String extension, Set<String> acceptedExtensions) {
        this.mimeType = mimeType;
        this.extension = extension;
        this.acceptedExtensions = acceptedExtensions;
    }

    public String mimeType() { return mimeType; }
    public String extension() { return extension; }
    public boolean isImage() { return this != PDF; }
    public boolean acceptsExtension(String ext) { return acceptedExtensions.contains(ext); }

    public static DetectedFileType detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return PNG;
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return WEBP;
        }
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return PDF;
        }
        return null;
    }
}
