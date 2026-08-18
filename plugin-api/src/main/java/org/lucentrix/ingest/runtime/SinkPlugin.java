package org.lucentrix.ingest.runtime;

import org.lucentrix.ingest.ContentChange;
import org.lucentrix.ingest.Sink;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.util.List;

@Extension
public interface SinkPlugin extends Sink<ContentChange>, ExtensionPoint {

    void push(List<ContentChange> documents);
}
