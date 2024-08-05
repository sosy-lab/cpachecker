// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.Immutable;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

/** A Multimap-implementation based on the {@link PathCopyingPersistentTreeMap}. */
@Immutable(containerOf = {"K", "V"})
public class PersistentMultimapWithPersistentValues<
    K extends Comparable<? super K>, V extends Comparable<? super V>> {

  private final PersistentMap<K, PersistentSet<V>> delegate;

  private PersistentMultimapWithPersistentValues(PersistentMap<K, PersistentSet<V>> pDelegate) {
    delegate = pDelegate;
  }

  public static <K extends Comparable<? super K>, V extends Comparable<? super V>>
      PersistentMultimapWithPersistentValues<K, V> of() {
    return new PersistentMultimapWithPersistentValues<K, V>(PathCopyingPersistentTreeMap.of());
  }

  public PersistentMultimapWithPersistentValues<K, V> putAndCopy(K key, V value) {
    return putAllAndCopy(key, Collections.singleton(value));
  }

  public PersistentMultimapWithPersistentValues<K, V> putAllAndCopy(K key, Iterable<V> values) {
    PersistentSet<V> old = delegate.get(key);
    if (old == null) {
      old = PersistentSet.of();
    }
    for (V value : values) {
      old = old.addAndCopy(value);
    }
    return new PersistentMultimapWithPersistentValues<>(delegate.putAndCopy(key, old));
  }

  public PersistentMultimapWithPersistentValues<K, V> putAllAndCopy(
      PersistentMultimapWithPersistentValues<K, V> other) {
    PersistentMultimapWithPersistentValues<K, V> tmp = this;
    for (Entry<K, PersistentSet<V>> entry : other.entries()) {
      tmp = tmp.putAllAndCopy(entry.getKey(), entry.getValue());
    }
    return tmp;
  }

  public PersistentMultimapWithPersistentValues<K, V> removeAndCopy(K key) {
    return new PersistentMultimapWithPersistentValues<>(delegate.removeAndCopy(key));
  }

  public PersistentMultimapWithPersistentValues<K, V> removeAndCopy(K key, V value) {
    PersistentSet<V> old = delegate.get(key);
    if (old == null || !old.contains(value)) {
      return this;
    }
    old = old.removeAndCopy(value);
    if (old.isEmpty()) {
      return new PersistentMultimapWithPersistentValues<>(delegate.removeAndCopy(key));
    } else {
      return new PersistentMultimapWithPersistentValues<>(delegate.putAndCopy(key, old));
    }
  }

  public PersistentSet<V> get(K key) {
    PersistentSet<V> set = delegate.get(key);
    return set == null ? PersistentSet.of() : set;
  }

  public boolean contains(K key, V value) {
    return delegate.containsKey(key) && delegate.get(key).contains(value);
  }

  public ImmutableSet<V> values() {
    return ImmutableSet.copyOf(Iterables.concat(delegate.values()));
  }

  public int size() {
    return delegate.size();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PersistentMultimapWithPersistentValues
        && delegate.equals(((PersistentMultimapWithPersistentValues<?, ?>) o).delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  public Set<Entry<K, PersistentSet<V>>> entries() {
    return delegate.entrySet();
  }
}
