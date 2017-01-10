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
package org.sosy_lab.cpachecker.cpa.arg.graphexport;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

/** an immutable map of facts about the transition. */
public class TransitionCondition implements Comparable<TransitionCondition> {

  private static final TransitionCondition EMPTY = new TransitionCondition();

  private final EnumMap<KeyDef, String> keyValues;

  private int hashCode = 0;

  private TransitionCondition() {
    keyValues = new EnumMap<>(KeyDef.class);
  }

  private TransitionCondition(EnumMap<KeyDef, String> pKeyValues) {
    keyValues = pKeyValues;
  }

  public TransitionCondition putAndCopy(final KeyDef pKey, final String pValue) {
    if (pValue.equals(keyValues.get(pKey))) {
      return this;
    }
    EnumMap<KeyDef, String> newMap = keyValues.clone();
    newMap.put(pKey, pValue);
    return new TransitionCondition(newMap);
  }

  public TransitionCondition putAllAndCopy(TransitionCondition tc) {
    EnumMap<KeyDef, String> newMap = null;
    for (Entry<KeyDef, String> e : keyValues.entrySet()) {
      if (!tc.keyValues.containsKey(e.getKey())) {
        if (newMap == null) {
          newMap = tc.keyValues.clone();
        }
        newMap.put(e.getKey(), e.getValue());
      }
    }
    return newMap == null ? tc : new TransitionCondition(newMap);
  }

  public TransitionCondition removeAndCopy(final KeyDef pKey) {
    if (keyValues.containsKey(pKey)) {
      EnumMap<KeyDef, String> newMap = keyValues.clone();
      newMap.remove(pKey);
      return new TransitionCondition(newMap);
    } else {
      return this;
    }
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof TransitionCondition
        && this.keyValues.equals(((TransitionCondition)pOther).keyValues);
  }

  public Map<KeyDef, String> getMapping() {
    return keyValues;
  }

  public boolean hasTransitionRestrictions() {
    return !keyValues.isEmpty();
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = keyValues.hashCode();
    }
    return hashCode;
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
              && (keyDef.equals(KeyDef.ASSUMPTIONSCOPE) || keyDef.equals(KeyDef.ASSUMPTIONRESULTFUNCTION)))
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

  public static TransitionCondition empty() {
    return EMPTY;
  }
}