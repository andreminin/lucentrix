package org.lucentrix.ingest.runtime;


import org.lucentrix.ingest.PersistableIterator;
import org.lucentrix.ingest.ChangePage;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

@Extension
public interface SourcePlugin extends PersistableIterator<ChangePage>, ExtensionPoint {

}
