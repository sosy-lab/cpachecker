// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ForwardingNavigableMap;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Map;
import java.util.NavigableMap;
import org.sosy_lab.cpachecker.util.OrderStatisticSet.OrderStatisticsSetProxy;

public interface OrderStatisticMap<K, V> extends NavigableMap<K, V> {

  /**
   * Returns the element of this set with the given rank. The lowest element in the set has rank ==
   * 0, the largest element in the set has rank == size - 1.
   *
   * @param pIndex the rank of the element to return
   * @return the element of this set with the given rank
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   */
  K getKeyByRank(int pIndex);

  Map.Entry<K, V> getEntryByRank(int pIndex);

  /**
   * Remove the element of this set with the given rank and return it.
   *
   * <p>The lowest element in the set has rank == 0, the largest element in the set has rank == size
   * - 1.
   *
   * @param pIndex the rank of the element to remove
   * @return the removed element
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   * @see #getKeyByRank(int)
   */
  @CanIgnoreReturnValue
  K removeByRank(int pIndex);

  /**
   * Return the rank of the given element in this set. Returns -1 if the element does not exist in
   * the set.
   *
   * <p>The lowest element in the set has rank == 0, the largest element in the set has rank == size
   * - 1.
   *
   * @param pObj the element to return the rank for
   * @return the rank of the given element in the set, or -1 if the element is not in the set
   * @throws NullPointerException if the given element is <code>null</code>
   */
  int rankOf(K pObj);

  @Override
  OrderStatisticMap<K, V> descendingMap();

  @Override
  OrderStatisticSet<K> navigableKeySet();

  @Override
  OrderStatisticSet<K> descendingKeySet();

  @Override
  OrderStatisticMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

  @Override
  OrderStatisticMap<K, V> headMap(K toKey, boolean inclusive);

  @Override
  OrderStatisticMap<K, V> tailMap(K fromKey, boolean inclusive);

  @Override
  OrderStatisticMap<K, V> subMap(K fromKey, K toKey);

  @Override
  OrderStatisticMap<K, V> headMap(K toKey);

  @Override
  OrderStatisticMap<K, V> tailMap(K fromKey);

  class OrderStatisticsMapProxy<K, V> extends ForwardingNavigableMap<K, V>
      implements OrderStatisticMap<K, V> {

    private final NavigableMap<K, V> delegate;

    public OrderStatisticsMapProxy(NavigableMap<K, V> pDelegate) {
      delegate = pDelegate;
    }

    @Override
    protected NavigableMap<K, V> delegate() {
      return delegate;
    }

    @Override
    public K getKeyByRank(int pIndex) {
      return new OrderStatisticsSetProxy<>(delegate.navigableKeySet()).getByRank(pIndex);
    }

    @Override
    public Entry<K, V> getEntryByRank(int pIndex) {
      K key = getKeyByRank(pIndex);
      return Maps.immutableEntry(key, get(key));
    }

    @Override
    @CanIgnoreReturnValue
    public K removeByRank(int pIndex) {
      K key = getKeyByRank(pIndex);
      V val = remove(key);
      assert val != null : "Key could be retrieved by rank, but no (or null) value associated";
      return key;
    }

    @Override
    public int rankOf(K pObj) {
      return new OrderStatisticsSetProxy<>(delegate.navigableKeySet()).rankOf(pObj);
    }

    @Override
    public OrderStatisticSet<K> navigableKeySet() {
      return new OrderStatisticsSetProxy<>(super.navigableKeySet());
    }

    @Override
    public OrderStatisticSet<K> descendingKeySet() {
      return new OrderStatisticsSetProxy<>(super.descendingKeySet());
    }

    @Override
    public OrderStatisticMap<K, V> descendingMap() {
      return new OrderStatisticsMapProxy<>(super.descendingMap());
    }

    @Override
    public OrderStatisticMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return new OrderStatisticsMapProxy<>(
          super.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override
    public OrderStatisticMap<K, V> headMap(K toKey, boolean inclusive) {
      return new OrderStatisticsMapProxy<>(super.headMap(toKey, inclusive));
    }

    @Override
    public OrderStatisticMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return new OrderStatisticsMapProxy<>(super.tailMap(fromKey, inclusive));
    }

    @Override
    public OrderStatisticMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public OrderStatisticMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public OrderStatisticMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }
  }
}
