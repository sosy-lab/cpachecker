package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.tests;

import com.google.common.collect.ForwardingMap;
import java.util.HashMap;
import java.util.Map;

public class ExpectMap<K, L> extends ForwardingMap<K, L> {

    private Map<K, L> map;

    public ExpectMap() {
        map = new HashMap<>();
    }

    public ExpectMap<K, L> expect(K key, L value) {
        put(key, value);
        return this;
    }

    @Override
    protected Map<K, L> delegate() {
        return null;
    }
}
