package com.karinderya_proto.catalog;

/** The JSON body for creating or updating an item. Fields may be missing, so they are objects. */
public record ItemRequest(String name, String category, Double price, Boolean available) {
}
