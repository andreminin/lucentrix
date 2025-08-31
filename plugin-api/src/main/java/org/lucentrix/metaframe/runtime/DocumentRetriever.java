package org.lucentrix.metaframe.runtime;


import org.lucentrix.metaframe.PersistableIterator;
import org.lucentrix.metaframe.DocumentPage;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

@Extension
public interface DocumentRetriever extends PersistableIterator<DocumentPage>, ExtensionPoint {

}
