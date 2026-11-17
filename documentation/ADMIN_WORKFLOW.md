# Administrator Workflows

## 0. First administrator

There's no admin sign-up page. At startup, `AdminBootstrapRunner` reads `ECOSWAP_ADMIN_EMAIL`, `ECOSWAP_ADMIN_PASSWORD` and optionally `ECOSWAP_ADMIN_NAME`. If no account with that e-mail exists, it creates an ACTIVE, verified ADMIN. If an account with that e-mail already exists, nothing changes. The password must meet the normal policy. Remove the variables once the account exists.

## 1. Registration queue

```mermaid
flowchart LR
    N[Notification: new registration] --> Q[/admin/pending-users]
    Q --> R{Review name, e-mail, university, date, e-mail verified}
    R -- Approve --> A[ACTIVE] --> L1[approval_history + audit log + student notified]
    R -- Reject + reason --> J[REJECTED, reason stored] --> L1
    U[/admin/users] --> S{Active student}
    S -- Suspend + reason --> SP[SUSPENDED; live sessions end] --> L1
    SP -- Reactivate --> A
```

If `ECOSWAP_REQUIRE_EMAIL_VERIFICATION=true`, unverified registrations can't be approved.

## 2. Listing queue

```mermaid
flowchart LR
    N[Notification: new listing] --> Q[/admin/pending-listings]
    Q --> R{Photos + details}
    R -- Approve --> A[APPROVED + AVAILABLE → Explore]
    R -- Reject + reason --> J[REJECTED, hidden]
    R -- Request changes + note --> C[NEEDS_CHANGES, hidden]
    All[/admin/listings] --> M{Any listing}
    M -- Suspend + reason --> S[SUSPENDED, hidden, pending requests closed]
    M -- Archive prohibited content --> X[ARCHIVED]
    A & J & C & S & X --> L[approval_history + audit log + owner notified]
```

## 3. Monitoring and intervention

- **Exchanges** (`/admin/exchanges`): read-only monitoring of every request, with filters, participants, offered item and dates. Individual requests never need approval. Admins act through reports, by suspending the listing or user.
- **Reports** (`/admin/reports`): OPEN → *Start review* (UNDER_REVIEW) → *Resolve* (optionally suspend the listing and/or the user, with notes) or *Dismiss* (notes). Every decision is audited.
- **Support** (`/admin/support`): search and filter, read the conversation, reply (the student is notified), set status (OPEN, IN_PROGRESS, WAITING_FOR_USER, RESOLVED, CLOSED).
- **Categories** (`/admin/categories`): add, edit, activate or deactivate. Deletion is only allowed when no listing references the category.
- **Audit logs** (`/admin/audit-logs`): every action above, with admin, action, target, reason and time. The log is read-only, with no edit or delete paths in the application.
