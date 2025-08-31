package org.lucentrix.metaframe.metadata.mapping;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.FieldType;
import org.lucentrix.metaframe.util.BiMap;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ToString
@EqualsAndHashCode
public class PlainSerde implements FieldSerde {

    private final BiMap<String, Field<?>> stringFieldBiMap = new BiMap<>();

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    @Override
    public String toString(Field<?> field) {
        Objects.requireNonNull(field);

        readLock.lock();
        try {
            return stringFieldBiMap.getKeyOrDefault(field, field.getName());
        } finally {
            readLock.unlock();
        }
    }

    public Field<?> fromString(String fieldName) {
        Objects.requireNonNull(fieldName);

        readLock.lock();
        try {
            return stringFieldBiMap.getValueOrDefault(fieldName, Field.of(fieldName));
        } finally {
            readLock.unlock();
        }
    }

    public Field<?> fromString(String fieldName, Object detectFromValue) {
        Objects.requireNonNull(fieldName);

        readLock.lock();
        try {
            Field<?> field = stringFieldBiMap.getValue(fieldName);
            if(field != null) {
                return field;
            }
        } finally {
            readLock.unlock();
        }

        writeLock.lock();
        try {
            Field<?> field = Field.of(fieldName, FieldType.detectByValue(detectFromValue));
            String name = toString(field);

            // Double-check
            stringFieldBiMap.computeIfValueAbsent(name, n -> field);

            return field;
        } finally {
            writeLock.unlock();
        }
    }
}
