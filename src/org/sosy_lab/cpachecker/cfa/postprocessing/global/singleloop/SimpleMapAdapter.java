/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * Instances of this class are used to adapt a given Map implementation to
 * the simple map interface.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 */
class SimpleMapAdapter<K, V> implements SimpleMap<K, V> {

  /**
   * The backing map implementation.
   */
  private final Map<K, V> adaptee;

  /**
   * Adapts the given map implementation.
   *
   * @param pAdaptee the map to adapt.
   */
  private SimpleMapAdapter(Map<K, V> pAdaptee) {
    Preconditions.checkNotNull(pAdaptee);
    adaptee = pAdaptee;
  }

  @Override
  public V get(Object pKey) {
    return adaptee.get(pKey);
  }

  @Override
  public V put(K pKey, V pValue) {
    return adaptee.put(pKey, pValue);
  }

  @Override
  public boolean containsKey(K pKey) {
    return adaptee.containsKey(pKey);
  }

  @Override
  public void clear() {
    adaptee.clear();
  }

  @Override
  public void putAllInto(SimpleMap<K, V> pTarget) {
    for (Map.Entry<K, V> entry : adaptee.entrySet()) {
      pTarget.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Adapts the given map as a simple map.
   *
   * @param pAdaptee the map to adapt.
   *
   * @return the adapter.
   */
  public static <S, T> SimpleMap<S, T> adapt(Map<S, T> pAdaptee) {
    return new SimpleMapAdapter<>(pAdaptee);
  }

  /**
   * Creates a new simple hash map.
   *
   * @return a new simple hash map.
   */
  public static <S, T> SimpleMap<S, T> createSimpleHashMap() {
    return adapt(new LinkedHashMap<S, T>());
  }

  @Override
  public boolean isEmpty() {
    return adaptee.isEmpty();
  }

  @Override
  public String toString() {
    return adaptee.toString();
  }

}