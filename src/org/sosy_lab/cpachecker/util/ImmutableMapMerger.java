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
package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Hm maybe I should migrate to immutable maps for other classes as well.
 */
public class ImmutableMapMerger {
  public interface MergeFunc<V> {
    V apply(V a, V b);
  }

  public interface MergeFuncWithKey<K, V> {
    V apply(K key, V a, V b);
  }

  /**
   * Join two maps taking the key into account, use [func] on duplicate values.
   * @param a input map
   * @param b input map
   * @param func function to merge two values from different maps.
   * @return map containing the union of keys in [a] and [b], which
   * contains the value from [a] if it contains only in [a], value from [b]
   * if it is contained only in [b] and [func] applied on value from [a]
   * and a value from [b] otherwise.
   */
  public static <K, V> ImmutableMap<K, V> merge(
      ImmutableMap<K, V> a,
      ImmutableMap<K, V> b,
      MergeFuncWithKey<K, V> func) {
    Set<K> allKeys = new HashSet<>();

    allKeys.addAll(a.keySet());
    allKeys.addAll(b.keySet());

    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

    // Well hey now we have to use the builders.

    for (K key : allKeys) {
      if (a.containsKey(key) && !b.containsKey(key)) {
        builder.put(key, a.get(key));
      } else if (!a.containsKey(key) && b.containsKey(key)) {
        builder.put(key, b.get(key));
      } else {
        builder.put(key, func.apply(key, a.get(key), b.get(key)));
      }
    }
    return builder.build();
  }

  /**
   * Like {@link :merge} but without taking keys into account.
   */
  public static <K, V> ImmutableMap<K, V> merge(
      ImmutableMap<K, V> a,
      ImmutableMap<K, V> b,
      MergeFunc<V> func) {
    Set<K> allKeys = new HashSet<>();

    allKeys.addAll(a.keySet());
    allKeys.addAll(b.keySet());

    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

    for (K key : allKeys) {
      if (a.containsKey(key) && !b.containsKey(key)) {
        builder.put(key, a.get(key));
      } else if (!a.containsKey(key) && b.containsKey(key)) {
        builder.put(key, b.get(key));
      } else {
        builder.put(key, func.apply(a.get(key), b.get(key)));
      }
    }
    return builder.build();
  }
}
