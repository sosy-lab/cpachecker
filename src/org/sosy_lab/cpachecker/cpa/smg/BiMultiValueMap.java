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
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Created by Vasilyev Anton on 05.02.16.
 */
public class BiMultiValueMap<K, V> implements Map<K, V> {

  private Map<K, V> internalMap;
  private Multimap<Object, K> internalOppositeMap;
  private ReturnOppositeValueOperator<V> oppositeValueOperator;

  public BiMultiValueMap(ReturnOppositeValueOperator<V> pOperator) {
    oppositeValueOperator = pOperator;
    internalMap = new HashMap<>();
    internalOppositeMap = ArrayListMultimap.create();
  }

  public BiMultiValueMap() {
    this(new ReturnOppositeSameOperator<V>());
  }

  @Override
  public int size() {
    return internalMap.size();
  }

  @Override
  public boolean isEmpty() {
    return internalMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return internalMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return internalMap.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return internalMap.get(key);
  }

  @Override
  public V put(K key, V value) {
    internalOppositeMap.put(oppositeValueOperator.getOpposite(value), key);
    return internalMap.put(key, value);
  }

  @Override
  public V remove(Object key) {
    V value =  internalMap.remove(key);
    if (value != null) {
      internalOppositeMap.remove(oppositeValueOperator.getOpposite(value), key);
    }
    return value;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    internalMap.putAll(m);
    for (Entry<? extends K, ? extends V> e : m.entrySet()) {
      internalOppositeMap.put(oppositeValueOperator.getOpposite(e.getValue()), e.getKey());
    }
  }

  @Override
  public void clear() {
    internalMap.clear();
    internalOppositeMap.clear();
  }

  @Override
  public Set<K> keySet() {
    return internalMap.keySet();
  }

  @Override
  public Collection<V> values() {
    return internalMap.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return internalMap.entrySet();
  }

  public void removeValue(V value) {
    for (K key : internalOppositeMap.removeAll(oppositeValueOperator.getOpposite(value))) {
      internalMap.remove(key);
    }
  }

  public void removeOppositValue(Object value) {
    for (K key : internalOppositeMap.removeAll(value)) {
      internalMap.remove(key);
    }
  }

  public Map<K, V> getMap() {
    return internalMap;
  }

  @Override
  public String toString() {
    return internalMap.toString();
  }
}

class ReturnOppositeSameOperator<V> implements ReturnOppositeValueOperator<V> {

  @Override
  public Object getOpposite(V value) {
    return value;
  }
}


