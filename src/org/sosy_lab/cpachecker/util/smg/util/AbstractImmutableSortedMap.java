// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.errorprone.annotations.Var;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract class AbstractImmutableSortedMap<K, V> extends AbstractImmutableMap<K, V>
    implements OurSortedMap<K, V> {

  @Override
  public boolean equals(@Nullable Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (!(pObj instanceof Map)) {
      return false;
    }
    @Var Map<?, ?> other = (Map<?, ?>) pObj;
    if (size() != other.size()) {
      return false;
    }

    // Order is irrelevant for the comparison, but if the order is the same
    // (or the same as ours, but reversed), we can do a linear comparison
    @Var boolean hasSameOrder = false;
    if (other instanceof SortedMap<?, ?>) {
      @SuppressWarnings("JdkObsolete") // want to handle other SortedMaps here
      Comparator<?> comparator = ((SortedMap<?, ?>) other).comparator();
      if (Collections3.guaranteedSameOrder(comparator(), comparator)) {
        hasSameOrder = true;
      } else if (other instanceof NavigableMap<?, ?>) {
        NavigableMap<?, ?> descendingOther = ((NavigableMap<?, ?>) other).descendingMap();

        if (Collections3.guaranteedSameOrder(comparator(), descendingOther.comparator())) {
          hasSameOrder = true;
          other = descendingOther;
        }
      }
    }

    if (hasSameOrder) {
      Iterator<?> it1 = this.entrySet().iterator();
      Iterator<?> it2 = other.entrySet().iterator();
      try {
        while (it1.hasNext()) {
          if (!it1.next().equals(it2.next())) {
            return false;
          }
        }
        return true;
      } catch (NoSuchElementException e) {
        return false; // concurrent change to other map
      }
    }

    return this.entrySet().equals(other.entrySet());
  }

  @Override
  public int hashCode() {
    return entrySet().hashCode();
  }

  private @Nullable K keyOrNull(@Nullable Entry<K, V> entry) {
    return entry == null ? null : entry.getKey();
  }

  private K keyOrNSE(@Nullable Entry<K, V> entry) {
    if (entry == null) {
      throw new NoSuchElementException();
    }
    return entry.getKey();
  }

  @Override
  public final K firstKey() {
    return keyOrNSE(firstEntry());
  }

  @Override
  public final K lastKey() {
    return keyOrNSE(lastEntry());
  }

  @Override
  public final K ceilingKey(K pKey) {
    return keyOrNull(ceilingEntry(pKey));
  }

  @Override
  public final K floorKey(K pKey) {
    return keyOrNull(floorEntry(pKey));
  }

  @Override
  public final K higherKey(K pKey) {
    return keyOrNull(higherEntry(pKey));
  }

  @Override
  public final K lowerKey(K pKey) {
    return keyOrNull(lowerEntry(pKey));
  }

  @Override
  public final OurSortedMap<K, V> headMap(K pToKey) {
    return headMap(pToKey, /*pInclusive=*/ false);
  }

  @Override
  public final OurSortedMap<K, V> tailMap(K pFromKey) {
    return tailMap(pFromKey, /*pInclusive=*/ true);
  }

  @Override
  public final OurSortedMap<K, V> subMap(K pFromKey, K pToKey) {
    return subMap(pFromKey, /*pFromInclusive=*/ true, pToKey, /*pToInclusive=*/ false);
  }

  @Override
  public final NavigableSet<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    return new SortedMapKeySet<>(this);
  }

  @Override
  public final NavigableSet<K> descendingKeySet() {
    return descendingMap().navigableKeySet();
  }

  @Override
  public final Entry<K, V> pollFirstEntry() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Entry<K, V> pollLastEntry() {
    throw new UnsupportedOperationException();
  }
}
