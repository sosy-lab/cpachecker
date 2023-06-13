// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.errorprone.annotations.Immutable;
import java.util.Iterator;
import java.util.Map.Entry;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

/**
 * A Stack-implementation based on the {@link PathCopyingPersistentTreeMap}. The runtime is
 * O(log(n)) instead of O(1) for a default stack operation.
 */
@Immutable(containerOf = "T")
public class PersistentStack<T> implements Iterable<T> {

  private final PersistentMap<Integer, T> delegate;

  private PersistentStack(PersistentMap<Integer, T> pDelegate) {
    delegate = pDelegate;
  }

  public static <T> PersistentStack<T> of() {
    return new PersistentStack<>(PathCopyingPersistentTreeMap.of());
  }

  public PersistentStack<T> pushAndCopy(T elem) {
    Preconditions.checkState(!delegate.containsKey(delegate.size() + 1), "level already exists");
    return new PersistentStack<>(delegate.putAndCopy(delegate.size() + 1, elem));
  }

  public PersistentStack<T> popAndCopy() {
    Preconditions.checkState(!delegate.isEmpty(), "there is no element");
    return new PersistentStack<>(delegate.removeAndCopy(delegate.size()));
  }

  public T peek() {
    Preconditions.checkState(!delegate.isEmpty(), "there is no first element");
    return delegate.get(delegate.size());
  }

  /** replace the first entry where the predicate is valid with the given element. */
  public PersistentStack<T> replace(Predicate<T> pred, T elem) {
    for (Entry<Integer, T> entry : delegate.entrySet()) {
      if (pred.apply(entry.getValue())) {
        int index = entry.getKey();
        if (elem == entry.getValue()) {
          return this;
        } else {
          return new PersistentStack<>(delegate.removeAndCopy(index).putAndCopy(index, elem));
        }
      }
    }
    throw new AssertionError("no match found");
  }

  public int size() {
    return delegate.size();
  }

  @Override
  public Iterator<T> iterator() {
    return delegate.values().iterator();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PersistentStack && delegate.equals(((PersistentStack<?>) o).delegate);
  }

  @Override
  public String toString() {
    return "[" + Joiner.on(", ").join(delegate.values()) + "]";
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }
}
