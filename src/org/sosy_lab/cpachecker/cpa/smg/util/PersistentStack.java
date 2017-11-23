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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.util.Iterator;
import java.util.Map.Entry;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

/**
 * A Stack-implementation based on the {@link PathCopyingPersistentTreeMap}. The runtime is
 * O(log(n)) instead of O(1) for a default stack operation.
 */
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
