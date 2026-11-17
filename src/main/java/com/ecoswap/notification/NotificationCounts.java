package com.ecoswap.notification;

/** Live header badge values (unread notifications, requests waiting for the owner's answer). */
public record NotificationCounts(long unread, long pendingReceivedRequests, long unreadMessages) {}
