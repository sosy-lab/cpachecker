/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.builders;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Multiset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public class SetMultiMapBuilder<K, V> implements SetMultimap<K, V> {
  private ImmutableMultimap<K, V> origMap;
  private SetMultimap<K, V> toAdd;
  private SetMultimap<K, V> toRemove;
  private int sizeCoef = 5;


  public SetMultiMapBuilder(ImmutableMultimap<K, V> pOrigMap) {
    origMap = pOrigMap;
    toAdd = HashMultimap.create();
    toRemove = HashMultimap.create();
  }

  public SetMultiMapBuilder(int pSizeCoef) {
    sizeCoef = pSizeCoef;
    origMap = ImmutableMultimap.of();
    toAdd = HashMultimap.create();
    toRemove = HashMultimap.create();
  }

  public SetMultiMapBuilder(SetMultiMapBuilder<K, V> pSetMultiMapBuilder) {
    sizeCoef = pSetMultiMapBuilder.sizeCoef;
    if (pSetMultiMapBuilder.origMap.size() > sizeCoef * (pSetMultiMapBuilder.toAdd.size() +
        pSetMultiMapBuilder.toRemove.size())) {
      origMap = build(pSetMultiMapBuilder);
      toAdd = HashMultimap.create();
      toRemove = HashMultimap.create();
    } else {
      origMap = pSetMultiMapBuilder.origMap;
      toAdd = HashMultimap.create(pSetMultiMapBuilder.toAdd);
      toRemove = HashMultimap.create(pSetMultiMapBuilder.toRemove);
    }
  }

  public void build() {
//    origMap = build(this);
//    toAdd = HashMultimap.create();
//    toRemove = HashMultimap.create();
  }

  private ImmutableMultimap<K, V> build(SetMultiMapBuilder<K, V> pSetMultiMapBuilder) {
    Builder<K, V> builder = ImmutableMultimap.builder();
    if (pSetMultiMapBuilder.toRemove.isEmpty()) {
      builder.putAll(pSetMultiMapBuilder.origMap);
      builder.putAll(pSetMultiMapBuilder.toAdd);
    } else {
      for (Entry<K, V> entry: pSetMultiMapBuilder.origMap.entries()) {
        if (!pSetMultiMapBuilder.toRemove.containsEntry(entry.getKey(), entry.getValue()))
          builder.put(entry);
      }
      builder.putAll(pSetMultiMapBuilder.toAdd);
    }
    return builder.build();
  }

  @Override
  public int size() {
    return origMap.size() + toAdd.size() - toRemove.size();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  /*
   * Casts are checked by containsKey()
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean containsKey(@Nullable Object pO) {
    Multiset<V> resultSet = HashMultiset.create();
    if (toAdd.containsKey(pO))
      resultSet.addAll(toAdd.get((K) pO));
    if (origMap.containsKey(pO)) {
      resultSet.addAll(origMap.get((K) pO));
    }
    if (toRemove.containsKey(pO)) {
      resultSet.removeAll(toRemove.get((K) pO));
    }
    return !resultSet.isEmpty();
  }

  private int countValues(Multimap<K, V> pMap, int pCount, @Nullable Object pO) {
    int res = pCount;
    for (V value : pMap.values()) {
      if (value.equals(pO)) {
        if (res++ > 0)
          return res;
      }
    }
    return res;
  }

  @Override
  public boolean containsValue(@Nullable Object pO) {
    int count = 0;
    for (V value : toRemove.values()) {
      if (value.equals(pO)) {
        count--;
      }
    }
    count = countValues(toAdd, count, pO);
    if (count > 0) {
      return true;
    }
    if (count == 0) {
      return origMap.containsValue(pO);
    } else {
      count = countValues(origMap, count, pO);
      if (count > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsEntry(@Nullable Object pO, @Nullable Object pO1) {
    boolean res = toAdd.containsEntry(pO, pO1);
    if (!res) {
      res = !toRemove.containsEntry(pO, pO1);
      if (res) {
        res = origMap.containsEntry(pO, pO1);
      }
    }
    return res;
  }

  @Override
  public boolean put(@Nullable K pK, @Nullable V pV) {
    boolean res = origMap.containsEntry(pK, pV);
    res = res || toRemove.remove(pK, pV);
    if (!res)
      res = toAdd.put(pK, pV);
    return res;
  }

  /*
   * Cast is checked by containsEntry()
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean remove(@Nullable Object pO, @Nullable Object pO1) {
    boolean res = toAdd.remove(pO, pO1);
    if (!res && origMap.containsEntry(pO, pO1)) {
      res = toRemove.put((K)pO, (V)pO1);
    }
    return res;
  }

  @Override
  public boolean putAll(@Nullable K pK, Iterable<? extends V> pIterable) {
    boolean res = false;
    for (V value : pIterable) {
      if (put(pK, value))
        res = true;
    }
    return res;
  }

  @Override
  public boolean putAll(Multimap<? extends K, ? extends V> pMultimap) {
    boolean res = false;
    for (Entry<? extends K, ? extends V> entry : pMultimap.entries()) {
      if (put(entry.getKey(), entry.getValue())) {
        res = true;
      }
    }
    return res;
  }


  @Override
  public Set<V> get(@Nullable K pK) {
    HashSet<V> result = new HashSet<>();
    result.addAll(origMap.get(pK));
    result.addAll(toAdd.get(pK));
    result.removeAll(toRemove.get(pK));
    return result;
  }

  @Override
  public Set<K> keySet() {
    Multiset<K> keys = keys();
    return keys.elementSet();
  }

  @Override
  public Multiset<K> keys() {
    Multiset<K> keys = HashMultiset.create();
    keys.addAll(origMap.keys());
    keys.addAll(toAdd.keys());
    keys.removeAll(toRemove.keys());
    return keys;
  }

  @Override
  public Collection<V> values() {
    Multiset<V> values = HashMultiset.create();
    values.addAll(origMap.values());
    values.addAll(toAdd.values());
    values.removeAll(toRemove.values());
    return values;
  }

  /*
   * Cast is checked by containsKey()
   */
  @SuppressWarnings("unchecked")
  @Override
  public Set<V> removeAll(@Nullable Object pO) {
    HashSet<V> result = new HashSet<>(toAdd.removeAll(pO));
    if (origMap.containsKey(pO)) {
      for (V value : origMap.get((K)pO)) {
        if (toRemove.put((K) pO, value)) {
          result.add(value);
        }
      }
    }
    return result;
  }

  @Override
  public void clear() {
    toAdd.clear();
    toRemove = HashMultimap.create(origMap);
  }

  @Override
  public Set<V> replaceValues(K pK, Iterable<? extends V> pIterable) {
    Set<V> toRemoveValues = get(pK);
    for (V value : pIterable) {
      if (toRemoveValues.contains(value)) {
        toRemoveValues.remove(value);
      } else {
        put(pK, value);
      }
    }
    for (V removed : toRemoveValues) {
      remove(pK, removed);
    }
    return toRemoveValues;
  }

  @Override
  public Set<Entry<K, V>> entries() {
    Set<Entry<K, V>> entries = new HashSet<>(origMap.entries());
    entries.addAll(toAdd.entries());
    entries.removeAll(toRemove.entries());
    return entries;
  }

  @Override
  public Map<K, Collection<V>> asMap() {
    Map<K, Collection<V>> mapView = new HashMap<>();
    for (K key : keySet()) {
      mapView.put(key, get(key));
    }
    return mapView;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SetMultiMapBuilder) {
      Set<Entry<K, V>> entries = entries();
      for (Object otherEntry : ((SetMultiMapBuilder<?, ?>) obj).entries()) {
        if (!entries.contains(otherEntry)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(origMap, toAdd, toRemove);
  }
}
