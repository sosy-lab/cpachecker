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
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

/**
 * A Set-implementation based on the {@link PathCopyingPersistentTreeMap}. We use {@code null} as
 * internal value for the map.
 */
@Immutable(containerOf = "K")
public class PersistentSet<K> implements Iterable<K> {

  private final PersistentMap<K, Void> delegate;

  private PersistentSet(PersistentMap<K, Void> pDelegate) {
    delegate = pDelegate;
  }

  public static <K extends Comparable<? super K>> PersistentSet<K> of() {
    return new PersistentSet<K>(PathCopyingPersistentTreeMap.of());
  }

  @SuppressFBWarnings
  public PersistentSet<K> addAndCopy(K key) {
    return new PersistentSet<>(delegate.putAndCopy(key, null));
  }

  public PersistentSet<K> removeAndCopy(K key) {
    return new PersistentSet<>(delegate.removeAndCopy(key));
  }

  public boolean contains(K key) {
    return delegate.containsKey(key);
  }

  public int size() {
    return delegate.size();
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  public Set<K> asSet() {
    return delegate.keySet();
  }

  @Override
  public Iterator<K> iterator() {
    return delegate.keySet().iterator();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PersistentSet)) { return false; }
    @SuppressWarnings("unchecked")
    PersistentSet<K> other = (PersistentSet<K>) o;
    return delegate.equals(other.delegate);
  }

  @Override
  public String toString() {
    return delegate.keySet().toString();
  }
}
