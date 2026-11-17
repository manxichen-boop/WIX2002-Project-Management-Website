package com.ecoswap.approval;

import com.ecoswap.user.entity.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records moderation decisions; always joins the caller's transaction. */
@Service
public class ApprovalService {

    private final ApprovalHistoryRepository history;

    public ApprovalService(ApprovalHistoryRepository history) {
        this.history = history;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(ApprovalEntityType type, Long entityId, User admin, ApprovalDecision decision, String reason) {
        history.save(new ApprovalHistory(type, entityId, admin, decision, reason));
    }

    @Transactional(readOnly = true)
    public List<ApprovalHistoryView> historyOf(ApprovalEntityType type, Long entityId) {
        return history.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(type, entityId).stream().map(ApprovalHistoryView::of).toList();
    }
}
