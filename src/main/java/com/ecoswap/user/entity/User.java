package com.ecoswap.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 190, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "profile_image_path", length = 255)
    private String profileImagePath;

    @Column(nullable = false, length = 150)
    private String university;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.PENDING_APPROVAL;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected User() {}

    /** Public registration can only ever create USER accounts in PENDING_APPROVAL. */
    public static User newStudent(String fullName, String email, String passwordHash, String university) {
        User u = new User();
        u.fullName = fullName;
        u.email = email;
        u.passwordHash = passwordHash;
        u.university = university;
        u.role = UserRole.USER;
        u.accountStatus = AccountStatus.PENDING_APPROVAL;
        return u;
    }

    /** Used only by the admin bootstrap process. */
    public static User newAdministrator(String fullName, String email, String passwordHash) {
        User u = new User();
        u.fullName = fullName;
        u.email = email;
        u.passwordHash = passwordHash;
        u.university = "EcoSwape Administration";
        u.role = UserRole.ADMIN;
        u.accountStatus = AccountStatus.ACTIVE;
        u.emailVerified = true;
        return u;
    }

    public boolean isAdmin() { return role == UserRole.ADMIN; }
    public boolean isActive() { return accountStatus == AccountStatus.ACTIVE; }

    public void changeStatus(AccountStatus status, String reason) {
        this.accountStatus = status;
        this.statusReason = reason;
    }

    public void markEmailVerified() { this.emailVerified = true; }
    public void changePassword(String newHash) { this.passwordHash = newHash; }
    public void rename(String fullName) { this.fullName = fullName; }
    public void changeUniversity(String university) { this.university = university; }
    public void changeProfileImage(String path) { this.profileImagePath = path; }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getProfileImagePath() { return profileImagePath; }
    public String getUniversity() { return university; }
    public UserRole getRole() { return role; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public String getStatusReason() { return statusReason; }
    public boolean isEmailVerified() { return emailVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
