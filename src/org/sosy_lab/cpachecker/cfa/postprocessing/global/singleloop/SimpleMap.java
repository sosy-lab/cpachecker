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

/**
 * Instances of implementing classes provide a smaller, more light weight
 * interface of common map operations than the Map interface.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 */
interface SimpleMap<K, V> {

  /**
   * Gets the value mapped to the given key.
   *
   * @param pKey the key.
   *
   * @return the value mapped to the given key.
   */
  V get(Object pKey);

  /**
   * Checks if the map is empty.
   *
   * @return {@code true} if the map is empty, {@code false} otherwise.
   */
  boolean isEmpty();

  /**
   * Maps the given value to the given key.
   *
   * @param pKey the key.
   * @param pValue the value.
   *
   * @return the value previously mapped to the key, if any, otherwise
   * {@code null}.
   */
  V put(K pKey, V pValue);

  /**
   * Checks if the given key is contained in the map.
   *
   * @param pKey the key.
   *
   * @return @return {@code true} if the key is contained, {@code false}
   * otherwise.
   */
  boolean containsKey(K pKey);

  /**
   * Removes all entries from the map.
   */
  void clear();

  /**
   * Puts all entries of the map into the given map.
   *
   * @param pTarget the target map.
   */
  public void putAllInto(SimpleMap<K, V> pTarget);

}