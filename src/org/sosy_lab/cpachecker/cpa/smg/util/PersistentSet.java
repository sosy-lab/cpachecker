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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

/** A Set-implementation based on the {@link PathCopyingPersistentTreeMap}.
 * We use {@code null} as internal value for the map. */
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

  public boolean contains(Object key) {
    return delegate.containsKey(key);
  }

  public int size() {
    return delegate.size();
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
    return delegate.toString();
  }
}
