package com.ecoswap.user.repository;

import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.entity.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    long countByRoleAndAccountStatus(UserRole role, AccountStatus status);

    List<User> findByRoleAndAccountStatus(UserRole role, AccountStatus status);

    /** Student accounts for the admin tables, with optional status and name/e-mail/university search. */
    @Query("""
            select u from User u
            where u.role = com.ecoswap.user.entity.UserRole.USER
              and (:status is null or u.accountStatus = :status)
              and (:q is null or lower(u.fullName) like :q or lower(u.email) like :q or lower(u.university) like :q)
            """)
    Page<User> searchStudents(@Param("status") AccountStatus status, @Param("q") String likePattern, Pageable pageable);

    @Query("select count(u) from User u where u.role = com.ecoswap.user.entity.UserRole.USER and u.createdAt >= :since")
    long countStudentsRegisteredSince(@Param("since") java.time.LocalDateTime since);

    @Query("select u.createdAt from User u where u.role = com.ecoswap.user.entity.UserRole.USER and u.createdAt >= :since")
    List<java.time.LocalDateTime> registrationTimesSince(@Param("since") java.time.LocalDateTime since);
}
