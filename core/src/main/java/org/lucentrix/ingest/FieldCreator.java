package org.lucentrix.ingest;

import org.javatuples.Pair;
import java.util.function.Function;
import org.lucentrix.ingest.metadata.FieldObjectMap;
import org.lucentrix.ingest.metadata.field.Field;

public interface FieldCreator extends Function<FieldObjectMap, Pair<Field<?>, Object>> {
}
