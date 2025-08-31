package org.lucentrix.metaframe.runtime;

import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.Target;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.util.List;

@Extension
public interface DocumentConsumer extends Target<LxEvent>, ExtensionPoint {

    void push(List<LxEvent> documents);
}
