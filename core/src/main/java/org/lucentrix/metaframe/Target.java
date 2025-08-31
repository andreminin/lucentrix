package org.lucentrix.metaframe;

import java.util.List;

public interface Target<T> {

    void push(List<T> items);
}
