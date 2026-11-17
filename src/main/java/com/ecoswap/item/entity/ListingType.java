package com.ecoswap.item.entity;

public enum ListingType {
    SWAP("Swap"), GIVEAWAY("Giveaway"), UPCYCLING("Upcycling");

    private final String label;

    ListingType(String label) { this.label = label; }

    public String label() { return label; }

    /** Only swaps require the requester to offer one of their own items. */
    public boolean requiresOfferedItem() { return this == SWAP; }
}
