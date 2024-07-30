// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

class HappensBeforeUtils {
  private HappensBeforeUtils() {}

  /** Creates a new Map resolving to a Set, with a new element added to a set. */
  static <Key, Value> Map<Key, Set<Value>> extendMapOfSets(
      final Map<Key, Set<Value>> base, final Key key, final Value value) {
    final ImmutableMap.Builder<Key, Set<Value>> newMap = ImmutableMap.builder();
    newMap.putAll(base);
    newMap.put(
        key,
        ImmutableSet.<Value>builder()
            .addAll(base.getOrDefault(key, ImmutableSet.of()))
            .add(value)
            .build());
    return newMap.buildKeepingLast();
  }

  /** Creates a new Map resolving to a List, with a new element added to a list. */
  static <Key, Value> Map<Key, List<Value>> extendMapOfLists(
      final Map<Key, List<Value>> base, final Key key, final Value value) {
    final ImmutableMap.Builder<Key, List<Value>> newMap = ImmutableMap.builder();
    newMap.putAll(base);
    newMap.put(
        key,
        listAndElement(base.getOrDefault(key, ImmutableList.of()), value));
    return newMap.buildKeepingLast();
  }

  /** Creates a new Map resolving to a List, with a new element inserted into a list. */
  static <Key, Value> Map<Key, List<Value>> insertIntoMapOfLists(
      final Map<Key, List<Value>> base, final int idx, final Key key, final Value value) {
    final ImmutableMap.Builder<Key, List<Value>> newMap = ImmutableMap.builder();
    newMap.putAll(base);
    final var oldList = base.getOrDefault(key, ImmutableList.of());
    checkArgument(oldList.size() >= idx, "Cannot insert into list with smaller size than index!");
    newMap.put(
        key,
        ImmutableList.<Value>builder()
            .addAll(oldList.subList(0, idx))
            .add(value)
            .addAll(oldList.subList(idx, oldList.size()))
            .build());
    return newMap.buildKeepingLast();
  }

  /** Creates a new Map resolving to a Set, with the specified element remove from a set. */
  static <Key, Value> Map<Key, Set<Value>> subtractFromMapOfSets(
      final Map<Key, Set<Value>> base, final Key key, final Value value) {
    checkArgument(
        base.containsKey(key) && base.get(key).contains(value),
        "Cannot remove non-existent element");
    final ImmutableMap.Builder<Key, Set<Value>> newMap = ImmutableMap.builder();
    newMap.putAll(base);
    newMap.put(
        key,
        Sets.difference(base.getOrDefault(key, ImmutableSet.of()), ImmutableSet.of(value))
            .immutableCopy());
    return newMap.buildKeepingLast();
  }

  /** Creates a new Map resolving to a Set, with the specified element remove from a set. */
  static <Key, Value> Map<Key, Set<Value>> subtractFromMapOfSets(
      final Map<Key, Set<Value>> base, final Key key, final Collection<Value> value) {
    if (value.isEmpty()) {
      return base;
    }
    checkArgument(
        base.containsKey(key) && base.get(key).containsAll(value),
        "Cannot remove non-existent element");
    final ImmutableMap.Builder<Key, Set<Value>> newMap = ImmutableMap.builder();
    newMap.putAll(base);
    newMap.put(
        key,
        Sets.difference(base.getOrDefault(key, ImmutableSet.of()), ImmutableSet.copyOf(value))
            .immutableCopy());
    return newMap.buildKeepingLast();
  }
}
