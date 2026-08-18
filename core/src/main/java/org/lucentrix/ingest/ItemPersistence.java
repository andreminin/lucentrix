package org.lucentrix.ingest;

import org.lucentrix.ingest.metadata.HasId;

public interface ItemPersistence {

    //Saves and return saved item persistence id
    <T extends HasId> void save(T obj);

    boolean delete(String id);

    <T> T load(String id, Class<T> type);
}
