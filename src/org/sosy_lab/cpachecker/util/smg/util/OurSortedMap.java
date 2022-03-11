// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Extension of {@link NavigableMap} that specifies {@link NavigableSet} as type of some collection
 * views (instead of {@link java.util.Set}).
 */
interface OurSortedMap<K, V> extends NavigableMap<K, V> {

  Iterator<Entry<K, V>> entryIterator();

  Iterator<Entry<K, V>> descendingEntryIterator();

  @Nullable Entry<K, V> getEntry(Object pKey);

  @Override
  NavigableSet<K> keySet();

  @Override
  NavigableSet<Map.Entry<K, V>> entrySet();

  @Override
  OurSortedMap<K, V> descendingMap();

  @Override
  OurSortedMap<K, V> subMap(K pFromKey, K pToKey);

  @Override
  OurSortedMap<K, V> subMap(K pFromKey, boolean pFromInclusive, K pToKey, boolean pToInclusive);

  @Override
  OurSortedMap<K, V> headMap(K pToKey);

  @Override
  OurSortedMap<K, V> headMap(K pToKey, boolean pInclusive);

  @Override
  OurSortedMap<K, V> tailMap(K pFromKey);

  @Override
  OurSortedMap<K, V> tailMap(K pFromKey, boolean pInclusive);

  @Immutable(containerOf = {"K", "V"})
  @SuppressWarnings("AvoidDefaultSerializableInInnerClasses") // class is (implicitly) static
  final class EmptyImmutableOurSortedMap<K extends Comparable<? super K>, V>
      extends AbstractImmutableSortedMap<K, V> implements Serializable {

    private static final long serialVersionUID = -4621218089650593459L;

    private static final OurSortedMap<?, ?> INSTANCE = new EmptyImmutableOurSortedMap<>();

    @SuppressWarnings("unchecked")
    static <K extends Comparable<? super K>, V> OurSortedMap<K, V> of() {
      return (OurSortedMap<K, V>) INSTANCE;
    }

    @Override
    public Comparator<? super K> comparator() {
      return null;
    }

    @Override
    public Iterator<Entry<K, V>> entryIterator() {
      return Collections.emptyIterator();
    }

    @Override
    public Iterator<Entry<K, V>> descendingEntryIterator() {
      return Collections.emptyIterator();
    }

    @Override
    public @Nullable Entry<K, V> getEntry(Object pKey) {
      return null;
    }

    @Override
    public OurSortedMap<K, V> subMap(
        K pFromKey, boolean pFromInclusive, K pToKey, boolean pToInclusive) {
      return this;
    }

    @Override
    public OurSortedMap<K, V> headMap(K pToKey, boolean pInclusive) {
      return this;
    }

    @Override
    public OurSortedMap<K, V> tailMap(K pFromKey, boolean pInclusive) {
      return this;
    }

    @Override
    public Entry<K, V> firstEntry() {
      return null;
    }

    @Override
    public Entry<K, V> lastEntry() {
      return null;
    }

    @Override
    public Entry<K, V> ceilingEntry(K pKey) {
      return null;
    }

    @Override
    public Entry<K, V> floorEntry(K pKey) {
      return null;
    }

    @Override
    public Entry<K, V> higherEntry(K pKey) {
      return null;
    }

    @Override
    public Entry<K, V> lowerEntry(K pKey) {
      return null;
    }

    @Override
    public ImmutableSortedSet<K> navigableKeySet() {
      return ImmutableSortedSet.of();
    }

    @Override
    public ImmutableList<V> values() {
      return ImmutableList.of();
    }

    @Override
    public OurSortedMap<K, V> descendingMap() {
      return this;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean equals(@Nullable Object pObj) {
      return pObj instanceof Map<?, ?> && ((Map<?, ?>) pObj).isEmpty();
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean containsKey(Object pKey) {
      return false;
    }

    @Override
    public boolean containsValue(Object pValue) {
      return false;
    }

    @Override
    public V get(Object pKey) {
      return null;
    }

    @Override
    public ImmutableSortedSet<Map.Entry<K, V>> entrySet() {
      return ImmutableSortedSet.of();
    }

    @Override
    public String toString() {
      return "{}";
    }
  }
}
