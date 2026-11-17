package com.ecoswap.impact;

import com.ecoswap.common.util.Times;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.UserRole;
import com.ecoswap.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Platform statistics for the homepage strip, the Impact page and the Community page. */
@Service
@Transactional(readOnly = true)
public class ImpactService {

    private final UserRepository users;
    private final ItemRepository items;
    private final ExchangeRequestRepository requests;

    public ImpactService(UserRepository users, ItemRepository items, ExchangeRequestRepository requests) {
        this.users = users;
        this.items = items;
        this.requests = requests;
    }

    public ImpactSummary summary() {
        List<ImpactSummary.Breakdown> byType = items.countReusedByListingType().stream()
                .map(r -> new ImpactSummary.Breakdown(((ListingType) r[0]).label(), (Long) r[1])).toList();
        List<ImpactSummary.Breakdown> byCategory = items.countReusedByCategory().stream()
                .map(r -> new ImpactSummary.Breakdown((String) r[0], (Long) r[1])).toList();
        return new ImpactSummary(
                users.countByRoleAndAccountStatus(UserRole.USER, AccountStatus.ACTIVE),
                requests.countByStatus(ExchangeRequestStatus.COMPLETED),
                items.countByAvailabilityStatus(ItemAvailabilityStatus.COMPLETED),
                items.sumReusedWeight().setScale(1, RoundingMode.HALF_UP),
                items.countReusedWithWeight(),
                items.countByApprovalStatusAndAvailabilityStatus(ListingApprovalStatus.APPROVED, ItemAvailabilityStatus.AVAILABLE),
                byType, byCategory);
    }

    /** Recently completed exchanges, anonymised. */
    public List<CommunityActivity> recentExchanges() {
        return requests.findTop6ByStatusOrderByCompletedAtDesc(ExchangeRequestStatus.COMPLETED).stream()
                .map(r -> new CommunityActivity("EXCHANGE_COMPLETED", r.getItem().getName(), r.getItem().getCategory().getName(),
                        r.getItem().getListingType().label(), Times.utc(r.getCompletedAt()), r.getItem().getId()))
                .toList();
    }
}
