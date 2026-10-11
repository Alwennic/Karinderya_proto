package com.karinderya_proto.catalog;

import java.util.List;
import java.util.Optional;

/** Database access for menu items. The tests replace this with an in-memory fake. */
public interface ItemRepository {
    List<Item> list();

    Optional<Item> find(long id);

    long create(String name, String category, double price, boolean available);

    /** Returns false if the item does not exist. */
    boolean update(long id, String name, String category, double price, boolean available);
}
