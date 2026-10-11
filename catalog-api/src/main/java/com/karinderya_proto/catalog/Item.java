package com.karinderya_proto.catalog;

/** One menu item, exactly as the frontend receives it as JSON. */
public record Item(long id, String name, String category, double price, boolean available) {
}
