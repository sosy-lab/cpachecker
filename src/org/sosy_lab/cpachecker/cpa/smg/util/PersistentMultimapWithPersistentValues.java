/*
 *  CPAchecker is a tool for configurable software verification.
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
public class PersistentMultimapWithPersistentValues<K extends Comparable<? super K>, V extends Comparable<? super V>> {

  private final PersistentMap<K, PersistentSet<V>> delegate;

  private PersistentMultimapWithPersistentValues(PersistentMap<K, PersistentSet<V>> pDelegate) {
    delegate = pDelegate;
  }

  public static <K extends Comparable<? super K>, V extends Comparable<? super V>> PersistentMultimapWithPersistentValues<K, V> of() {
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
    for (V value: values) {
      old = old.addAndCopy(value);
    }
    return new PersistentMultimapWithPersistentValues<>(delegate.putAndCopy(key, old));
  }

  public PersistentMultimapWithPersistentValues<K, V> putAllAndCopy(PersistentMultimapWithPersistentValues<K, V> other) {
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
        && delegate.equals(((PersistentMultimapWithPersistentValues<?,?>)o).delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  public Set<Entry<K, PersistentSet<V>>> entries() {
    return delegate.entrySet();
  }
}
