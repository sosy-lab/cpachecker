// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Equivalence;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@link NavigableSet} to be used as the entry set of a {@link NavigableMap}.
 *
 * <p>This implementation forwards all methods to the underlying map.
 */
@SuppressWarnings("BadImport") // want to import Map.Entry because this class is about Map
final class SortedMapEntrySet<K, V> extends AbstractSet<Entry<K, V>>
    implements NavigableSet<Entry<K, V>>, Serializable {

  private static final long serialVersionUID = 2891466632825409479L;

  private final OurSortedMap<K, V> map;

  SortedMapEntrySet(OurSortedMap<K, V> pMap) {
    map = checkNotNull(pMap);
  }

  @Override
  public boolean equals(@Nullable Object pO) {
    if (pO instanceof SortedMapEntrySet<?, ?>
        && Collections3.guaranteedSameOrder(
            this.map.comparator(), ((SortedMapEntrySet<?, ?>) pO).map.comparator())) {
      // Map has a linear comparison for this case
      return map.equals(((SortedMapEntrySet<?, ?>) pO).map);
    }
    return super.equals(pO);
  }

  @Override
  @SuppressWarnings("RedundantOverride") // to document that using super.hashCode is intended
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(@Nullable Object pO) {
    if (!(pO instanceof Entry)) {
      return false;
    }
    Entry<?, ?> other = (Entry<?, ?>) pO;
    Entry<?, ?> entry = map.getEntry(other.getKey());
    return entry != null && Objects.equals(entry.getValue(), other.getValue());
  }

  @Override
  public boolean containsAll(Collection<?> pC) {
    // This set is somewhat inconsistent: its order is based only on the entry keys,
    // but for equality (and contains) we need to check the keys and values of entries.
    // Because we know that this set and the Entry objects are well-behaved,
    // we can do the linear comparator-based comparison anyway
    // if we additionally compare equality of entries.
    return Collections3.sortedSetContainsAll(this, pC, Equivalence.equals());
  }

  @Override
  public Entry<K, V> ceiling(Entry<K, V> pE) {
    return map.ceilingEntry(pE.getKey());
  }

  @Override
  public Entry<K, V> first() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return map.firstEntry();
  }

  @Override
  public Entry<K, V> floor(Entry<K, V> pE) {
    return map.floorEntry(pE.getKey());
  }

  @Override
  public Entry<K, V> higher(Entry<K, V> pE) {
    return map.higherEntry(pE.getKey());
  }

  @Override
  public Entry<K, V> last() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return map.lastEntry();
  }

  @Override
  public Entry<K, V> lower(Entry<K, V> pE) {
    return map.lowerEntry(pE.getKey());
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return map.entryIterator();
  }

  @Override
  public Iterator<Entry<K, V>> descendingIterator() {
    return map.descendingMap().entryIterator();
  }

  @Override
  public NavigableSet<Entry<K, V>> descendingSet() {
    return map.descendingMap().entrySet();
  }

  @Override
  public boolean add(Entry<K, V> pE) {
    if (contains(pE)) {
      return false;
    }
    map.put(pE.getKey(), pE.getValue());
    return true;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Entry<K, V> pollFirst() {
    return map.pollFirstEntry();
  }

  @Override
  public Entry<K, V> pollLast() {
    return map.pollLastEntry();
  }

  @Override
  public boolean remove(@Nullable Object pO) {
    if (!(pO instanceof Entry)) {
      return false;
    }
    Entry<?, ?> entry = (Entry<?, ?>) pO;
    return map.remove(entry.getKey(), entry.getValue());
  }

  @Override
  public Comparator<? super Entry<K, V>> comparator() {
    if (map.comparator() == null) {
      @SuppressWarnings("unchecked") // K needs to be Comparable if map has no comparator
      Comparator<Entry<K, V>> comp =
          (Comparator<Entry<K, V>>) (Comparator<?>) Entry.comparingByKey();
      return comp;
    } else {
      return Entry.comparingByKey(map.comparator());
    }
  }

  @Override
  public NavigableSet<Entry<K, V>> subSet(
      Entry<K, V> pFromElement,
      boolean pFromInclusive,
      Entry<K, V> pToElement,
      boolean pToInclusive) {
    return map.subMap(pFromElement.getKey(), pFromInclusive, pToElement.getKey(), pToInclusive)
        .entrySet();
  }

  @Override
  public NavigableSet<Entry<K, V>> headSet(Entry<K, V> pToElement, boolean pInclusive) {
    return map.headMap(pToElement.getKey(), pInclusive).entrySet();
  }

  @Override
  public NavigableSet<Entry<K, V>> tailSet(Entry<K, V> pFromElement, boolean pInclusive) {
    return map.tailMap(pFromElement.getKey(), pInclusive).entrySet();
  }

  @Override
  public NavigableSet<Entry<K, V>> subSet(Entry<K, V> pFromElement, Entry<K, V> pToElement) {
    return map.subMap(pFromElement.getKey(), pToElement.getKey()).entrySet();
  }

  @Override
  public NavigableSet<Entry<K, V>> headSet(Entry<K, V> pToElement) {
    return map.headMap(pToElement.getKey()).entrySet();
  }

  @Override
  public NavigableSet<Entry<K, V>> tailSet(Entry<K, V> pFromElement) {
    return map.tailMap(pFromElement.getKey()).entrySet();
  }
}
