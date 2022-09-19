// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.util;

import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;

public class PersistentBiMap<K extends Comparable<? super K>, V extends Comparable<? super V>> {
  PersistentSortedMap<K, V> delegate;
  PersistentSortedMap<V, K> reverse;

  private PersistentBiMap(PersistentSortedMap<K, V> pDelegate, PersistentSortedMap<V, K> pReverse) {
    delegate = pDelegate;
    reverse = pReverse;
  }

  public static <K extends Comparable<? super K>, V extends Comparable<? super V>>
      PersistentBiMap<K, V> of() {
    return new PersistentBiMap<K, V>(
        PathCopyingPersistentTreeMap.of(), PathCopyingPersistentTreeMap.of());
  }

  public int size() {
    return delegate.size();
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  public boolean containsKey(Object pO) {
    return delegate.containsKey(pO);
  }

  public boolean containsValue(Object pO) {
    return reverse.containsKey(pO);
  }

  public V get(Object pO) {
    return delegate.get(pO);
  }

  public Set<Entry<K, V>> entrySet() {
    return delegate.entrySet();
  }

  public PersistentBiMap<V, K> inverse() {
    return new PersistentBiMap<>(reverse, delegate);
  }

  public PersistentBiMap<K, V> putAndCopy(K pK, V pV) {
    return new PersistentBiMap<>(delegate.putAndCopy(pK, pV), reverse.putAndCopy(pV, pK));
  }

  public PersistentBiMap<K, V> removeAndCopy(K pK) {
    V value = get(pK);
    if (value != null) {
      return removeAndCopy(pK, value);
    } else {
      return this;
    }
  }

  private PersistentBiMap<K, V> removeAndCopy(K pK, V pV) {
    return new PersistentBiMap<>(delegate.removeAndCopy(pK), reverse.removeAndCopy(pV));
  }
}
