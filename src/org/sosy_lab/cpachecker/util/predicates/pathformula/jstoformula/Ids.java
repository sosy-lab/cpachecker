/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages mapping of (static) values to IDs (integer). Some kind of values are encoded as an
 * integer formula (for example string literals). Each value has to be mapped to a unique integer.
 * This class manages this mapping including the generation of the unique IDs.
 *
 * @param <T> The type of value that is mapped to IDs.
 */
class Ids<T> {
  private final Map<T, Integer> ids;
  private int currentId;

  Ids() {
    currentId = 0;
    ids = new HashMap<>();
  }

  /**
   * Get ID of a value.
   *
   * @param pValue The value whose ID should be returned. If no ID exists yet then a new ID is
   *     created.
   * @return The ID associated with the passed value.
   */
  public int get(final T pValue) {
    if (ids.containsKey(pValue)) {
      return ids.get(pValue);
    }
    ++currentId;
    ids.put(pValue, currentId);
    return currentId;
  }

  @Override
  public String toString() {
    return "Ids{" + "ids=" + ids + '}';
  }
}
