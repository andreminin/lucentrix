package org.lucentrix.ingest;

import java.util.List;

public interface Sink<T> {

    void push(List<T> items);
}
