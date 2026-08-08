package io.github.easy4j.hermes;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDK 8 测试集合工厂，替代 JDK 9 引入的 List.of 与 Map.of。
 */
final class Java8Collections {

    private Java8Collections() {
    }

    @SafeVarargs
    static <T> List<T> list(T... values) {
        return Arrays.asList(values);
    }

    static <K, V> Map<K, V> map() {
        return Collections.emptyMap();
    }

    static <K, V> Map<K, V> map(K key, V value) {
        return Collections.singletonMap(key, value);
    }

    static <K, V> Map<K, V> map(K firstKey, V firstValue, K secondKey, V secondValue) {
        Map<K, V> values = new LinkedHashMap<>();
        values.put(firstKey, firstValue);
        values.put(secondKey, secondValue);
        return values;
    }
}
