// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;

class HappensBeforeUtils {
  private HappensBeforeUtils() {}

  /**
   * Creates a new Map resolving to a Set, with a new element added to a set.
   */
  static <Key, Value> Map<Key, Set<Value>> extendMapOfSets(final Map<Key, Set<Value>> base, final Key key, final Value value) {
    final ImmutableMap.Builder<Key, Set<Value>> newMap = ImmutableMap.builder();
    newMap.putAll(base);
    newMap.put(key, ImmutableSet.<Value>builder().addAll(base.getOrDefault(key, Set.of())).add(value).build());
    return newMap.buildKeepingLast();
  }

  /**
   * Creates a new Map resolving to a Set, with the specified element remove from a set.
   */
  static <Key, Value> Map<Key, Set<Value>> subtractMapOfSets(final Map<Key, Set<Value>> base, final Key key, final Value value) {
    checkArgument(base.containsKey(key) && base.get(key).contains(value), "Cannot remove non-existent element");
    final ImmutableMap.Builder<Key, Set<Value>> newMap = ImmutableMap.builder();
    newMap.putAll(base);
    newMap.put(key, Sets.difference(base.getOrDefault(key, Set.of()), ImmutableSet.of(value)).immutableCopy());
    return newMap.buildKeepingLast();
  }

}
