// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ForwardingNavigableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressFBWarnings(
    value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE",
    justification = "nullability depends on underlying map")
final class DescendingSortedMap<K, V> extends ForwardingNavigableMap<K, V>
    implements OurSortedMap<K, V>, Serializable {

  private static final long serialVersionUID = -3499934696704295393L;

  private final OurSortedMap<K, V> map;

  DescendingSortedMap(OurSortedMap<K, V> pMap) {
    map = checkNotNull(pMap);
  }

  @Override
  protected NavigableMap<K, V> delegate() {
    return map;
  }

  @Override
  public Iterator<Entry<K, V>> entryIterator() {
    return map.descendingEntryIterator();
  }

  @Override
  public Iterator<Entry<K, V>> descendingEntryIterator() {
    return map.entryIterator();
  }

  @Override
  public @Nullable Entry<K, V> getEntry(@Nullable Object pKey) {
    return map.getEntry(pKey);
  }

  @Override
  public Comparator<? super K> comparator() {
    return Collections.reverseOrder(map.comparator());
  }

  @Override
  public boolean equals(@Nullable Object pObject) {
    if (pObject instanceof DescendingSortedMap<?, ?>) {
      // order is irrelevant for equals, and after unwrapping comparison could be linear
      return map.equals(((DescendingSortedMap<?, ?>) pObject).map);
    }
    return map.equals(pObject);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public K firstKey() {
    return map.lastKey();
  }

  @Override
  public K lastKey() {
    return map.firstKey();
  }

  @Override
  public NavigableSet<Entry<K, V>> entrySet() {
    return new SortedMapEntrySet<>(this);
  }

  @Override
  public NavigableSet<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    return new SortedMapKeySet<>(this);
  }

  @Override
  public NavigableSet<K> descendingKeySet() {
    return map.navigableKeySet();
  }

  @Override
  public OurSortedMap<K, V> descendingMap() {
    return map;
  }

  @Override
  public Collection<V> values() {
    return new MapValues<>(this);
  }

  @Override
  public Entry<K, V> lowerEntry(@Nullable K pKey) {
    return map.higherEntry(pKey);
  }

  @Override
  public K lowerKey(@Nullable K pKey) {
    return map.higherKey(pKey);
  }

  @Override
  public Entry<K, V> floorEntry(@Nullable K pKey) {
    return map.ceilingEntry(pKey);
  }

  @Override
  public K floorKey(@Nullable K pKey) {
    return map.ceilingKey(pKey);
  }

  @Override
  public Entry<K, V> ceilingEntry(@Nullable K pKey) {
    return map.floorEntry(pKey);
  }

  @Override
  public K ceilingKey(@Nullable K pKey) {
    return map.floorKey(pKey);
  }

  @Override
  public Entry<K, V> higherEntry(@Nullable K pKey) {
    return map.lowerEntry(pKey);
  }

  @Override
  public K higherKey(@Nullable K pKey) {
    return map.lowerKey(pKey);
  }

  @Override
  public Entry<K, V> firstEntry() {
    return map.lastEntry();
  }

  @Override
  public Entry<K, V> lastEntry() {
    return map.firstEntry();
  }

  @Override
  public Entry<K, V> pollFirstEntry() {
    return map.pollLastEntry();
  }

  @Override
  public Entry<K, V> pollLastEntry() {
    return map.pollFirstEntry();
  }

  @Override
  public OurSortedMap<K, V> subMap(
      @Nullable K pFromKey, boolean pFromInclusive, @Nullable K pToKey, boolean pToInclusive) {
    return map.subMap(
            pToKey, /*pFromInclusive=*/ pToInclusive, pFromKey, /*pToInclusive=*/ pFromInclusive)
        .descendingMap();
  }

  @Override
  public OurSortedMap<K, V> headMap(@Nullable K pToKey, boolean pInclusive) {
    return map.tailMap(pToKey, /*pInclusive=*/ pInclusive).descendingMap();
  }

  @Override
  public OurSortedMap<K, V> tailMap(@Nullable K pFromKey, boolean pInclusive) {
    return map.headMap(pFromKey, /*pInclusive=*/ pInclusive).descendingMap();
  }

  @Override
  public OurSortedMap<K, V> headMap(@Nullable K pToKey) {
    return headMap(pToKey, /*pInclusive=*/ false);
  }

  @Override
  public OurSortedMap<K, V> tailMap(@Nullable K pFromKey) {
    return tailMap(pFromKey, /*pInclusive=*/ true);
  }

  @Override
  public OurSortedMap<K, V> subMap(@Nullable K pFromKey, @Nullable K pToKey) {
    return subMap(pFromKey, /*pFromInclusive=*/ true, pToKey, /*pToInclusive=*/ false);
  }

  @Override
  public String toString() {
    return standardToString();
  }
}
