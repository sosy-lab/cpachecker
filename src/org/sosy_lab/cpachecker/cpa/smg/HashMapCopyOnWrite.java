/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.collect.PersistentMap;

/**
 * Created by Anton Vasilyev on 04.02.16.
 */

public class HashMapCopyOnWrite<K, V> extends HashMap<K, V> implements PersistentMap<K, V> {
  private HashMap<K, V> internalHashMap;

  public HashMapCopyOnWrite(HashMapCopyOnWrite<K, V> pKVHashMapCopyOnWrite) {
    internalHashMap = new HashMap<>(pKVHashMapCopyOnWrite.internalHashMap);
  }

  public HashMapCopyOnWrite(int initialCapacity, float loadFactor) {
    internalHashMap = new HashMap<>(initialCapacity, loadFactor);
  }

  public HashMapCopyOnWrite(int initialCapacity) {
    internalHashMap = new HashMap<>(initialCapacity);
  }

  public HashMapCopyOnWrite() {
    internalHashMap = new HashMap<>();
  }

  public HashMapCopyOnWrite(Map<? extends K, ? extends V> m) {
    internalHashMap = new HashMap<>(m);
  }

  @Override
  public int size() {
    return internalHashMap.size();
  }

  @Override
  public boolean isEmpty() {
    return internalHashMap.isEmpty();
  }

  @Override
  public V get(Object key) {
    return internalHashMap.get(key);
  }

  @Override
  public boolean containsKey(Object key) {
    return internalHashMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return internalHashMap.containsValue(value);
  }

  @Override
  public Object clone() {
    return super.clone();
  }

  @Override
  public Set<K> keySet() {
    return internalHashMap.keySet();
  }

  @Override
  public Collection<V> values() {
    return internalHashMap.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return internalHashMap.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    return internalHashMap.equals(o);
  }

  @Override
  public int hashCode() {
    return internalHashMap.hashCode();
  }

  @Override
  public String toString() {
    return internalHashMap.toString();
  }

  /** @deprecated */
  @Deprecated
  public final void clear() {
    throw new UnsupportedOperationException();
  }

  /** @deprecated */
  @Deprecated
  public final V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  /** @deprecated */
  @Deprecated
  public final void putAll(Map<? extends K, ? extends V> pM) {
    throw new UnsupportedOperationException();
  }

  /** @deprecated */
  @Deprecated
  public final V remove(Object pKey) {
    throw new UnsupportedOperationException();
  }

  public HashMapCopyOnWrite<K, V> putAllAndCopy(Map<? extends K, ? extends V> var1) {
    HashMapCopyOnWrite<K, V> temp = new HashMapCopyOnWrite(this);
    temp.internalHashMap.putAll(var1);
    return temp;
  }

  @Override
  public HashMapCopyOnWrite<K, V> putAndCopy(K pK, V pV) {
    HashMapCopyOnWrite<K, V> temp = new HashMapCopyOnWrite(this);
    temp.internalHashMap.put(pK, pV);
    return temp;
  }

  @Override
  public HashMapCopyOnWrite<K, V> removeAndCopy(Object key) {
    if (this.containsKey(key)) {
      HashMapCopyOnWrite<K, V> temp = new HashMapCopyOnWrite(this);
      temp.internalHashMap.remove(key);
      return temp;
    } else {
      return this;
    }
  }

  public HashMapCopyOnWrite<K, V> removeKeysAndCopy(Set<? extends Object> keys) {
    if (keys.isEmpty()) {
      return this;
    } else {
      HashMapCopyOnWrite<K, V> temp = new HashMapCopyOnWrite(this);
      for (Object key : keys) {
        temp.internalHashMap.remove(key);
      }
      return temp;
    }
  }

  @Override
  public HashMapCopyOnWrite<K, V> empty() {
    HashMapCopyOnWrite<K, V> temp = new HashMapCopyOnWrite();
    return temp;
  }
}
