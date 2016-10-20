/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.graphExport;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

/** an immutable map of facts about the transition. */
public class TransitionCondition implements Comparable<TransitionCondition> {

  private final PersistentSortedMap<KeyDef, String> keyValues;

  public TransitionCondition() {
    keyValues = PathCopyingPersistentTreeMap.of();
  }

  private TransitionCondition(PersistentSortedMap<KeyDef, String> pKeyValues) {
    keyValues = pKeyValues;
  }

  public TransitionCondition putAndCopy(final KeyDef pKey, final String pValue) {
    return new TransitionCondition(keyValues.putAndCopy(pKey, pValue));
  }

  public TransitionCondition putAllAndCopy(TransitionCondition tc) {
    PersistentSortedMap<KeyDef, String> tmp = keyValues;
    for (Entry<KeyDef, String> e : tc.keyValues.entrySet()) {
      tmp = tmp.putAndCopy(e.getKey(), e.getValue());
    }
    return new TransitionCondition(tmp);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof TransitionCondition
        && this.keyValues.equals(((TransitionCondition)pOther).keyValues);
  }

  public PersistentSortedMap<KeyDef, String> getMapping() {
    return keyValues;
  }

  public boolean hasTransitionRestrictions() {
    return !keyValues.isEmpty();
  }

  @Override
  public int hashCode() {
    return keyValues.hashCode();
  }

  @Override
  public String toString() {
    return keyValues.toString();
  }

  public boolean summarizes(TransitionCondition pLabel) {
    if (equals(pLabel)) {
      return true;
    }
    boolean ignoreAssumptionScope =
        !keyValues.keySet().contains(KeyDef.ASSUMPTION)
            || !pLabel.keyValues.keySet().contains(KeyDef.ASSUMPTION);
    boolean ignoreInvariantScope =
        !keyValues.keySet().contains(KeyDef.INVARIANT)
        || !pLabel.keyValues.keySet().contains(KeyDef.INVARIANT);
    for (KeyDef keyDef : KeyDef.values()) {
      if (!keyDef.equals(KeyDef.ASSUMPTION)
          && !keyDef.equals(KeyDef.INVARIANT)
          && !(ignoreAssumptionScope
              && (keyDef.equals(KeyDef.ASSUMPTIONSCOPE) || keyDef.equals(KeyDef.RESULTFUNCTION)))
          && !(ignoreInvariantScope && keyDef.equals(KeyDef.INVARIANTSCOPE))
          && !Objects.equals(keyValues.get(keyDef), pLabel.keyValues.get(keyDef))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int compareTo(TransitionCondition pO) {
    if (this == pO) {
      return 0;
    }
    Iterator<Map.Entry<KeyDef, String>> entryIterator = keyValues.entrySet().iterator();
    Iterator<Map.Entry<KeyDef, String>> otherEntryIterator = pO.keyValues.entrySet().iterator();
    while (entryIterator.hasNext() && otherEntryIterator.hasNext()) {
      Map.Entry<KeyDef, String> entry = entryIterator.next();
      Map.Entry<KeyDef, String> otherEntry = otherEntryIterator.next();
      int compKey = entry.getKey().compareTo(otherEntry.getKey());
      if (compKey != 0) {
        return compKey;
      }
      int compVal = entry.getValue().compareTo(otherEntry.getValue());
      if (compVal != 0) {
        return compVal;
      }
    }
    if (!entryIterator.hasNext()) {
      return -1;
    }
    return 1;
  }
}