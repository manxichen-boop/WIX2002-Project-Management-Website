package com.ecoswap.item.entity;

public enum ItemCondition {
    NEW("New"), LIKE_NEW("Like new"), GOOD("Good"), FAIR("Fair"), WORN("Well used");

    private final String label;

    ItemCondition(String label) { this.label = label; }

    public String label() { return label; }
}
