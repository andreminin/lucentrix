package org.lucentrix.metaframe.util;

import lombok.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * This is class is not thread safe, use it for reading in multithreaded environments
 */

public class BiMap<K, V> {
    private final Map<K, V> valueByKey;
    private final Map<V, K> keyByValue;

    public BiMap() {
        valueByKey = new LinkedHashMap<>();
        keyByValue = new LinkedHashMap<>();
    }

    public BiMap(Map<K, V> valueByKey, Map<V, K> keyByValue) {
        this.valueByKey = new LinkedHashMap<>(valueByKey);
        this.keyByValue = new LinkedHashMap<>(keyByValue);
    }

    public void put(K key, V value) {
        valueByKey.put(key, value);
        keyByValue.put(value, key);
    }

    public V getValue(K key) {
        return valueByKey.get(key);
    }

    public V getValueOrDefault(K key, V value) {
        if(key == null) {
            return value;
        }
        return valueByKey.getOrDefault(key, value);
    }

    public K getKey(V value) {
        return keyByValue.get(value);
    }

    public K getKeyOrDefault(V value, K key) {
        if(value == null) {
            return key;
        }
        return keyByValue.getOrDefault(value, key);
    }

    public void removeByKey(K key) {
        V toRemove = valueByKey.remove(key);
        keyByValue.remove(toRemove);
    }

    public void removeByValue(V value) {
        K toRemove = keyByValue.remove(value);
        valueByKey.remove(toRemove);
    }

    public void removeIfMatch(K key, V value) {
        valueByKey.remove(key, value);
        keyByValue.remove(value, key);
    }

    public void computeIfValueAbsent(K key, Function<? super K, ? extends V> compute) {
        valueByKey.computeIfAbsent(key, compute);
        keyByValue.put(valueByKey.get(key), key);
    }

    public void computeIfKeyAbsent(V key, Function<? super V, ? extends K> compute) {
        keyByValue.computeIfAbsent(key, compute);
        valueByKey.put(keyByValue.get(key), key);
    }

    public static <K,V> BiMapBuilder<K, V> builder() {
        return new BiMapBuilder<>();
    }

    public static class BiMapBuilder<K, V> {
        private final Map<K, V> valueByKey;
        private final Map<V, K> keyByValue;

        public BiMapBuilder() {
            this.valueByKey = new LinkedHashMap<>();
            this.keyByValue = new LinkedHashMap<>();
        }

        public BiMapBuilder<K, V> put(K key, V value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            valueByKey.put(key, value);
            keyByValue.put(value, key);

            return this;
        }

        public BiMap<K,V> build() {
            return new BiMap<>(valueByKey, keyByValue);
        }
    }
}