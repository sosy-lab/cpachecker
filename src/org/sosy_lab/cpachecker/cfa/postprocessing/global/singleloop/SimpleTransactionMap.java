/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

/**
 * Instances of this class support transactions, i.e. all modifications to
 * the map are either uncommitted or committed, and operations to commit or
 * abort uncommitted changes are provided.
 *
 * This map does not support the {@code null} key or {@code null} values.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 */
class SimpleTransactionMap<K, V> implements SimpleMap<K, V> {

  /**
   * The committed map.
   */
  private final SimpleMap<K, V> persistentWrappee;

  /**
   * The uncommitted changes.
   */
  private final SimpleMap<K, V> transactionalWrappee;

  /**
   * Creates a new transaction map with the given backing maps for committed
   * and uncommitted changes.
   *
   * @param pPersistentWrappee the map holding the committed data.
   * @param pTransactionalWrappee the map holding the uncommitted changes.
   */
  public SimpleTransactionMap(SimpleMap<K, V> pPersistentWrappee, SimpleMap<K, V> pTransactionalWrappee) {
    persistentWrappee = pPersistentWrappee;
    transactionalWrappee = pTransactionalWrappee;
  }

  /**
   * Creates a new transaction map with the given backing map for committed
   * changes.
   *
   * @param pPersistentWrappee the map holding the committed data.
   */
  public SimpleTransactionMap(SimpleMap<K, V> pPersistentWrappee) {
    this(pPersistentWrappee, SimpleMapAdapter.<K, V>createSimpleHashMap());
  }

  @Override
  public V get(Object pKey) {
    V result = persistentWrappee.get(pKey);
    if (result != null) {
      return result;
    }
    return transactionalWrappee.get(pKey);
  }

  @Override
  public V put(@Nonnull K pKey, @Nonnull V pValue) {
    Preconditions.checkNotNull(pKey);
    Preconditions.checkNotNull(pValue);
    V previous = get(pKey);
    if (previous == null || !previous.equals(pValue)) {
      transactionalWrappee.put(pKey, pValue);
    }
    return previous;
  }

  @Override
  public boolean containsKey(K pKey) {
    return persistentWrappee.containsKey(pKey) || transactionalWrappee.containsKey(pKey);
  }

  /**
   * Commits all uncommitted changes.
   */
  public void commit() {
    transactionalWrappee.putAllInto(persistentWrappee);
    abort();
  }

  /**
   * Discards all uncommitted changes.
   */
  public void abort() {
    transactionalWrappee.clear();
  }

  /**
   * Removes all entries from the map and commits the modification.
   */
  @Override
  public void clear() {
    persistentWrappee.clear();
    transactionalWrappee.clear();
  }

  @Override
  public boolean isEmpty() {
    return persistentWrappee.isEmpty() && transactionalWrappee.isEmpty();
  }

  @Override
  public void putAllInto(SimpleMap<K, V> pTarget) {
    persistentWrappee.putAllInto(pTarget);
    transactionalWrappee.putAllInto(pTarget);
  }

  @Override
  public String toString() {
    return String.format("%s; (%s)", persistentWrappee, transactionalWrappee);
  }

}