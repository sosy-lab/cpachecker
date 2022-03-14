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
public class PersistentMultimap<K, V> {

  private final PersistentMap<K, ImmutableSet<V>> delegate;

  private PersistentMultimap(PersistentMap<K, ImmutableSet<V>> pDelegate) {
    delegate = pDelegate;
  }

  public static <K extends Comparable<? super K>, V> PersistentMultimap<K, V> of() {
    return new PersistentMultimap<K, V>(PathCopyingPersistentTreeMap.of());
  }

  public PersistentMultimap<K, V> putAndCopy(K key, V value) {
    return putAllAndCopy(key, Collections.singleton(value));
  }

  public PersistentMultimap<K, V> putAllAndCopy(K key, Iterable<V> values) {
    ImmutableSet.Builder<V> builder = ImmutableSet.builder();
    Set<V> old = delegate.get(key);
    if (old != null) {
      builder.addAll(old);
    }
    return new PersistentMultimap<>(delegate.putAndCopy(key, builder.addAll(values).build()));
  }

  public PersistentMultimap<K, V> putAllAndCopy(PersistentMultimap<K, V> other) {
    PersistentMultimap<K, V> tmp = this;
    for (Entry<K, ImmutableSet<V>> entry : other.entries()) {
      tmp = tmp.putAllAndCopy(entry.getKey(), entry.getValue());
    }
    return tmp;
  }

  public PersistentMultimap<K, V> removeAndCopy(K key) {
    return new PersistentMultimap<>(delegate.removeAndCopy(key));
  }

  public PersistentMultimap<K, V> removeAndCopy(K key, V value) {
    Set<V> old = delegate.get(key);
    if (old == null || !old.contains(value)) {
      return this;
    }
    ImmutableSet.Builder<V> builder = ImmutableSet.builder();
    builder.addAll(Iterables.filter(old, e -> !e.equals(value)));
    ImmutableSet<V> fresh = builder.build();
    if (fresh.isEmpty()) {
      return new PersistentMultimap<>(delegate.removeAndCopy(key));
    } else {
      return new PersistentMultimap<>(delegate.putAndCopy(key, fresh));
    }
  }

  public ImmutableSet<V> get(K key) {
    ImmutableSet<V> set = delegate.get(key);
    return set == null ? ImmutableSet.of() : set;
  }

  public boolean contains(K key) {
    return delegate.containsKey(key);
  }

  public boolean containsEntry(K key, V value) {
    return delegate.containsKey(key) && delegate.get(key).contains(value);
  }

  public ImmutableSet<V> values() {
    return ImmutableSet.copyOf(Iterables.concat(delegate.values()));
  }

  public int size() {
    return delegate.size();
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PersistentMultimap
        && delegate.equals(((PersistentMultimap<?, ?>) o).delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  public Set<Entry<K, ImmutableSet<V>>> entries() {
    return delegate.entrySet();
  }
}
