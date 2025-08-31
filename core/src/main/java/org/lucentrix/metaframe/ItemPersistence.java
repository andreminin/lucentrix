package org.lucentrix.metaframe;

import org.lucentrix.metaframe.metadata.Identity;

public interface ItemPersistence {

    //Saves and return saved item persistence id
    <T extends Identity> void save(T obj);

    boolean delete(String id);

    <T> T load(String id, Class<T> type);
}
