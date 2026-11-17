package com.ecoswap.storage;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemImage;
import com.ecoswap.item.repository.ItemImageRepository;
import com.ecoswap.support.SupportTicket;
import com.ecoswap.support.SupportTicketRepository;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The only way stored files reach a browser. Each endpoint checks who may see the file;
 * storage paths are never exposed. Unauthorised requests get 404 so file existence isn't leaked.
 */
@RestController
@RequestMapping("/media")
public class MediaController {

    private final ItemImageRepository images;
    private final UserRepository users;
    private final SupportTicketRepository tickets;
    private final StorageService storage;

    public MediaController(ItemImageRepository images, UserRepository users, SupportTicketRepository tickets, StorageService storage) {
        this.images = images;
        this.users = users;
        this.tickets = tickets;
        this.storage = storage;
    }

    @GetMapping("/items/{imageId:\\d+}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> itemImage(@PathVariable Long imageId, @AuthenticationPrincipal AuthenticatedUser me) {
        ItemImage image = images.findWithItem(imageId).orElseThrow(() -> new NotFoundException("Image"));
        Item item = image.getItem();
        boolean isPublic = item.isPubliclyVisible();
        boolean allowed = isPublic || (me != null && (me.isAdmin() || item.isOwnedBy(me.getId())));
        if (!allowed) {
            throw new NotFoundException("Image");
        }
        CacheControl cache = isPublic ? CacheControl.maxAge(Duration.ofDays(1)).cachePublic() : CacheControl.noStore();
        return file(image.getStorageKey(), image.getMimeType(), cache, null);
    }

    /** Profile photos are only shown to signed-in members. */
    @GetMapping("/profiles/{userId:\\d+}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> profileImage(@PathVariable Long userId) {
        User user = users.findById(userId).filter(u -> u.getProfileImagePath() != null)
                .orElseThrow(() -> new NotFoundException("Image"));
        String mime = user.getProfileImagePath().endsWith(".png") ? "image/png"
                : user.getProfileImagePath().endsWith(".webp") ? "image/webp" : "image/jpeg";
        return file(user.getProfileImagePath(), mime, CacheControl.maxAge(Duration.ofHours(1)).cachePrivate(), null);
    }

    /** Support attachments: the ticket owner and administrators only. */
    @GetMapping("/support/{ticketId:\\d+}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> supportAttachment(@PathVariable Long ticketId, @AuthenticationPrincipal AuthenticatedUser me) {
        SupportTicket ticket = tickets.findById(ticketId)
                .filter(t -> t.getAttachmentPath() != null)
                .filter(t -> me.isAdmin() || t.getUser().getId().equals(me.getId()))
                .orElseThrow(() -> new NotFoundException("Attachment"));
        return file(ticket.getAttachmentPath(), ticket.getAttachmentMimeType(), CacheControl.noStore(),
                ticket.getAttachmentOriginalFilename());
    }

    private ResponseEntity<Resource> file(String key, String mime, CacheControl cache, String downloadName) {
        Resource resource = storage.load(key);
        boolean pdf = "application/pdf".equals(mime);
        ContentDisposition disposition = (pdf ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(downloadName != null ? downloadName : key.substring(key.lastIndexOf('/') + 1)).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .cacheControl(cache)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
