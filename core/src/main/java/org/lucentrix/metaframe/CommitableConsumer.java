package org.lucentrix.metaframe;

import java.util.function.Consumer;

public interface CommitableConsumer<T> extends Consumer<T> {

    void commit(boolean force);
}
