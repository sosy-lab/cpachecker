// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A convenient way of representing iterable data structures as sets.
 *
 * <p>All modifying operations (e.g., {@link UnmodifiableSetView#add(Object)}, {@link
 * UnmodifiableSetView#remove(Object)}, etc.) throw an {@link UnsupportedOperationException}.
 * However, a set represented by a {@link UnmodifiableSetView} is not necessarily immutable, because
 * modifications to its underlying data structure are reflected in the set.
 *
 * <p>To implement an unmodifiable set view, this class needs to be extended and {@link
 * UnmodifiableSetView#iterator()} implemented. {@link UnmodifiableSetView#size()} should be
 * overridden if the subclass admits a more efficient implementation.
 *
 * <p>IMPORTANT: It's expected that the underlying data structure does not contain duplicates and at
 * most one {@code null} element. {@link Objects#equals(Object, Object)} must never return {@code
 * true} for any two elements returned during a single iteration of elements.
 */
abstract class UnmodifiableSetView<E> extends AbstractCollection<E> implements Set<E> {

  /**
   * Returns a set that contains all duplicate lists in the specified iterable.
   *
   * <p>Elements in such a duplicate list are equal to all other elements in the same list. Each
   * list contains at least two elements. Elements in a duplicate list are in the same order they
   * appear in the specified iterable.
   *
   * @param <E> the type of elements in the iterable
   * @param pIterable the iterable to find duplicates in
   * @return a set that contains all lists of duplicates in the specified iterable
   * @throws NullPointerException if {@code pIterable == null}
   */
  static <E> ImmutableSet<List<E>> duplicates(Iterable<E> pIterable) {

    int iterableSize = Iterators.size(pIterable.iterator());
    Set<E> set = new HashSet<>(iterableSize);
    Iterables.addAll(set, pIterable);

    if (set.size() < iterableSize) {

      // element -> all elements the element is equal to, including itself
      Multimap<E, E> occurrencesMap = ArrayListMultimap.create();
      pIterable.forEach(element -> occurrencesMap.put(element, element));

      ImmutableSet.Builder<List<E>> duplicatesBuilder = ImmutableSet.builder();
      for (Collection<E> occurrences : occurrencesMap.asMap().values()) {
        if (occurrences.size() > 1) {
          // we cannot use `ImmutableList`, because it doesn't allow `null` elements
          duplicatesBuilder.add(Collections.unmodifiableList(new ArrayList<>(occurrences)));
        }
      }

      return duplicatesBuilder.build();
    }

    return ImmutableSet.of();
  }

  @Override
  public int size() {
    // Using `Iterables.size(this)` leads to infinite recursion as it internally calls
    // `Collection#size()` for `Collection` instances.
    return Iterators.size(iterator());
  }

  @Override
  public final boolean add(E pElement) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean remove(Object pObject) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean addAll(Collection<? extends E> pCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean retainAll(Collection<?> pCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean removeAll(Collection<?> pCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void clear() {
    throw new UnsupportedOperationException();
  }
}
