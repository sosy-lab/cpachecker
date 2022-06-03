// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.util;

import com.google.errorprone.annotations.Immutable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

/**
 * A Set-implementation based on the {@link PathCopyingPersistentTreeMap}. We use {@code null} as
 * internal value for the map.
 */
@Immutable(containerOf = "K")
public class PersistentSet<K extends Comparable<? super K>> implements Set<K> {

  private final PersistentMap<K, Void> delegate;

  private PersistentSet(PersistentMap<K, Void> pDelegate) {
    delegate = pDelegate;
  }

  public PersistentSet() {
    delegate = PathCopyingPersistentTreeMap.of();
  }

  public static <K extends Comparable<? super K>> PersistentSet<K> of() {
    return new PersistentSet<K>(PathCopyingPersistentTreeMap.of());
  }

  public static <K extends Comparable<? super K>> PersistentSet<K> of(K entry) {
    PersistentSet<K> retSet = new PersistentSet<K>(PathCopyingPersistentTreeMap.of());
    return retSet.addAndCopy(entry);
  }

  public static <K extends Comparable<? super K>> PersistentSet<K> copyOf(Collection<K> entries) {
    PersistentSet<K> retSet = new PersistentSet<K>(PathCopyingPersistentTreeMap.of());
    for (K e : entries) {
      retSet = retSet.addAndCopy(e);
    }
    return retSet;
  }

  public static <K extends Comparable<? super K>> PersistentSet<K> copyOf(Iterable<K> entries) {
    PersistentSet<K> retSet = new PersistentSet<K>(PathCopyingPersistentTreeMap.of());
    for (K e : entries) {
      retSet = retSet.addAndCopy(e);
    }
    return retSet;
  }

  @SuppressFBWarnings
  public PersistentSet<K> addAndCopy(K key) {
    return new PersistentSet<>(delegate.putAndCopy(key, null));
  }

  public PersistentSet<K> removeAndCopy(K key) {
    return new PersistentSet<>(delegate.removeAndCopy(key));
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.containsKey(o);
  }

  public Set<K> asSet() {
    return delegate.keySet();
  }

  @Override
  public Iterator<K> iterator() {
    return delegate.keySet().iterator();
  }

  @Override
  public Object[] toArray() {
    return delegate.keySet().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(K pK) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object el : c) {
      if (!contains(el)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends K> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PersistentSet)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    PersistentSet<K> other = (PersistentSet<K>) o;
    return delegate.equals(other.delegate);
  }

  @Override
  public String toString() {
    return delegate.keySet().toString();
  }
}
