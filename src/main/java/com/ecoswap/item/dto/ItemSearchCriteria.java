package com.ecoswap.item.dto;

import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ListingType;

/** Explore filters. Every field is optional. sort = newest | oldest | name. */
public record ItemSearchCriteria(String q, String category, ItemCondition condition, ListingType type, String location,
                                 String sort, int page, int size) {}
