package org.lucentrix.metaframe;

import org.javatuples.Pair;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.metadata.field.Field;

public interface FieldCreator extends Processor<FieldObjectMap, Pair<Field<?>, Object>> {
}
