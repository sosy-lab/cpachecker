/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingNavigableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.SortedSet;

/**
 * A {@link SortedSet} that allows two additional operations: receiving (and deleting) an element by
 * its <i>rank</i>, and getting the rank of an element.
 *
 * <p>Implementations should adhere to all contracts of the <code>SortedSet</code> interface.
 *
 * <p>Implementing classes should provide two means for comparing elements:
 *
 * <ol>
 *   <li>Using the natural ordering of the elements. In this case, all elements of the set have to
 *       implement the {@link Comparable} interface.
 *   <li>Using a {@link java.util.Comparator Comparator} to create an order over the elements of the
 *       set.
 * </ol>
 *
 * In both cases, the used compare-method should be consistent with <code>equals</code>, i.e.,
 * <code>compare(a, b) == 0  =&gt;  a.equals(b)</code>, so that the contract provided by {@link
 * java.util.Set Set} is fulfilled. If the used compare-method is not consistent with <code>equals
 * </code>, the Set contract is not fulfilled.
 *
 * @param <T> the type of elements maintained by this set
 */
public interface OrderStatisticSet<T> extends NavigableSet<T> {

  /**
   * Returns the element of this set with the given rank. The lowest element in the set has rank ==
   * 0, the largest element in the set has rank == size - 1.
   *
   * <p>If the used compare-method is not consistent with <code>equals</code>, i.e., <code>
   * compare(a, b)</code> does <b>not</b> imply <code>a.equals(b)</code>, elements that were added
   * first will have a higher rank than elements equal by comparison that were added later.
   *
   * <p>Example:
   *
   * <pre>
   *     * Element type T: {@link java.awt.Point Point(x, y)}
   *     * Comparator: compare(a, b) = a.x - b.x
   *
   *     add(new Point(1, 1))
   *     add(new Point(2, 2))
   *     add(new Point(1, 3))
   *
   *     After these three operations, the set order will be:
   *     Pair.of(1, 3) - Pair.of(1, 1) - Pair.of(2, 2)
   *
   *     Thus:
   *     getKeyByRank(0) = Point(1, 3)
   *     getKeyByRank(1) = Point(1, 1)
   *     getKeyByRank(2) = Point(2, 2)
   *   </pre>
   *
   * @param pIndex the rank of the element to return
   * @return the element of this set with the given rank
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   */
  T getByRank(int pIndex);

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
   * @see #getByRank(int)
   */
  @CanIgnoreReturnValue
  T removeByRank(int pIndex);

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
  int rankOf(T pObj);

  @Override
  OrderStatisticSet<T> descendingSet();

  @Override
  OrderStatisticSet<T> subSet(
      T fromElement, boolean fromInclusive, T toElement, boolean toInclusive);

  @Override
  OrderStatisticSet<T> headSet(T toElement, boolean inclusive);

  @Override
  OrderStatisticSet<T> tailSet(T fromElement, boolean inclusive);

  @Override
  OrderStatisticSet<T> subSet(T fromElement, T toElement);

  @Override
  OrderStatisticSet<T> headSet(T toElement);

  @Override
  OrderStatisticSet<T> tailSet(T fromElement);

  class OrderStatisticsSetProxy<E> extends ForwardingNavigableSet<E>
      implements OrderStatisticSet<E> {

    private final NavigableSet<E> delegate;

    OrderStatisticsSetProxy(NavigableSet<E> pDelegate) {
      delegate = pDelegate;
    }

    @Override
    protected NavigableSet<E> delegate() {
      return delegate;
    }

    @Override
    public E getByRank(int pIndex) {
      return Iterables.get(delegate, pIndex);
    }

    @Override
    @CanIgnoreReturnValue
    public E removeByRank(int pIndex) {
      E elem = getByRank(pIndex);
      Preconditions.checkState(
          delegate.remove(elem), "Element could be retrieved, but not deleted");
      return elem;
    }

    @Override
    public int rankOf(E pObj) {
      return Iterables.indexOf(delegate, o -> compare(o, pObj) == 0);
    }

    @SuppressWarnings("unchecked")
    private int compare(E pO1, E pO2) {
      Comparator<? super E> comparator = comparator();
      if (comparator != null) {
        return comparator.compare(pO1, pO2);
      } else {
        return ((Comparable<E>) pO1).compareTo(pO2);
      }
    }

    @Override
    public OrderStatisticSet<E> descendingSet() {
      return new OrderStatisticsSetProxy<>(super.descendingSet());
    }

    @Override
    public OrderStatisticSet<E> subSet(
        E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
      return new OrderStatisticsSetProxy<>(
          super.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override
    public OrderStatisticSet<E> headSet(E toElement, boolean inclusive) {
      return new OrderStatisticsSetProxy<>(super.headSet(toElement, inclusive));
    }

    @Override
    public OrderStatisticSet<E> tailSet(E fromElement, boolean inclusive) {
      return new OrderStatisticsSetProxy<>(super.tailSet(fromElement, inclusive));
    }

    @Override
    public OrderStatisticSet<E> headSet(E toElement) {
      return headSet(toElement, false);
    }

    @Override
    public OrderStatisticSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public OrderStatisticSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
    }
  }
}
