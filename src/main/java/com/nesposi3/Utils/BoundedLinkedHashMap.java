package com.nesposi3.Utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A linked hashmap with a bound on size, provided in constructor
 * @param <K> Key type
 * @param <V> Value type
 */
public class BoundedLinkedHashMap<K,V> extends LinkedHashMap<K,V> {
    private int maxSize;
    public BoundedLinkedHashMap(int maxSize){
        super();
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size()>maxSize;
    }
}
