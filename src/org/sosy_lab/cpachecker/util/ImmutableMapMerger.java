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

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

public class ImmutableMapMerger {
  public interface MergeFunc<K, V> {
    V apply(K key, V a, V b);
  }

  /**
   * Join two maps taking the key into account, use {@code func} on duplicate values.
   * @param a input map
   * @param b input map
   * @param func function to merge two values from different maps.
   * @return map containing the union of keys in {@code a} and {@code b}, which
   * contains the value from {@code a} if it contains only in {@code a}, value
   * from {@code b} if it is contained only in {@code b} and {@code func}
   * applied on value from {@code a} and a value from {@code b} otherwise.
   */
  public static <K, V> ImmutableMap<K, V> merge(
      ImmutableMap<K, V> a,
      ImmutableMap<K, V> b,
      MergeFunc<K, V> func) {
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
        builder.put(key, func.apply(key, a.get(key), b.get(key)));
      }
    }
    return builder.build();
  }
}
