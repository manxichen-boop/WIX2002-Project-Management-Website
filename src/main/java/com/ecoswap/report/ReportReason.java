package com.ecoswap.report;

public enum ReportReason {
    INAPPROPRIATE_LISTING("Inappropriate listing"), MISLEADING_DESCRIPTION("Misleading description"),
    SUSPICIOUS_USER("Suspicious user"), PROHIBITED_CONTENT("Prohibited content"),
    INAPPROPRIATE_BEHAVIOUR("Inappropriate behaviour"), OTHER("Other");

    private final String label;

    ReportReason(String label) { this.label = label; }

    public String label() { return label; }
}
