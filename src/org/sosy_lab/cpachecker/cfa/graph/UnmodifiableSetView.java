// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.collect.Iterators;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * A convenient way of representing iterable data structures as sets.
 *
 * <p>All modifying operations (e.g., {@link UnmodifiableSetView#add(Object)}, {@link
 * UnmodifiableSetView#remove(Object)}, etc.) throw an {@link UnsupportedOperationException}, but
 * modifications to an underlying data structure will be reflected in its unmodifiable view.
 *
 * <p>To implement an unmodifiable set view, this class needs to be extended and {@link
 * UnmodifiableSetView#iterator()} implemented.
 *
 * <p>IMPORTANT: It's expected that the underlying data structure does not contain duplicates and at
 * most one {@code null} element ({@link Objects#equals(Object, Object)} must never return {@code
 * true} for any two elements returned during a single iteration of elements).
 */
abstract class UnmodifiableSetView<E> extends AbstractCollection<E> implements Set<E> {

  @Override
  public int size() {
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
