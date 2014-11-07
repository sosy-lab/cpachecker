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
package org.sosy_lab.cpachecker.cpa.invariants.formula.variablerelations;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class VariableRelationSet<ConstantType> implements Set<VariableRelation<ConstantType>> {

  private final Map<Object, VariableRelation<ConstantType>> backingMap;

  public VariableRelationSet() {
    this.backingMap = new HashMap<>();
  }

  public VariableRelationSet(Collection<VariableRelation<ConstantType>> pRelations) {
    this();
    addAll(pRelations);
  }

  public VariableRelationSet(VariableRelationSet<ConstantType> pOther) {
    this(pOther.backingMap);
  }

  private VariableRelationSet(Map<Object, VariableRelation<ConstantType>> pOtherBackingMap) {
    backingMap = new HashMap<>(pOtherBackingMap);
  }

  @Override
  public int size() {
    return backingMap.size();
  }

  @Override
  public boolean isEmpty() {
    return backingMap.isEmpty();
  }

  @Override
  public boolean contains(Object pO) {
    return backingMap.values().contains(pO);
  }

  @Override
  public Iterator<VariableRelation<ConstantType>> iterator() {
    return backingMap.values().iterator();
  }

  @Override
  public Object[] toArray() {
    return backingMap.values().toArray();
  }

  @Override
  public <T> T[] toArray(T[] pA) {
    return backingMap.values().toArray(pA);
  }

  @Override
  public boolean add(VariableRelation<ConstantType> pE) {
    return refineBy(pE);
  }

  public boolean uniteWith(VariableRelation<ConstantType> pE) {
    VariableRelation<ConstantType> previous = backingMap.get(pE.getCompatibilityKey());
    if (previous == null || previous.equals(pE)) {
      return false;
    }
    VariableRelation<ConstantType> newRelation = previous.union(pE);
    put(pE.getCompatibilityKey(), newRelation);
    return true;
  }

  public boolean uniteWith(VariableRelationSet<ConstantType> pOther) {
    boolean changed = backingMap.keySet().retainAll(pOther.backingMap.keySet());
    for (VariableRelation<ConstantType> other : pOther.backingMap.values()) {
      changed |= uniteWith(other);
    }
    return changed;
  }

  public boolean refineBy(Collection<VariableRelation<ConstantType>> pC) {
    boolean changed = false;
    for (VariableRelation<ConstantType> variableRelation : pC) {
      changed |= refineBy(variableRelation);
    }
    return changed;
  }

  public boolean refineBy(VariableRelation<ConstantType> pE) {
    VariableRelation<ConstantType> previous = backingMap.get(pE.getCompatibilityKey());
    if (previous == null) {
      backingMap.put(pE.getCompatibilityKey(), pE);
      return true;
    }
    VariableRelation<ConstantType> newRelation = previous.intersect(pE);
    if (previous.equals(newRelation)) {
      return false;
    }
    put(pE.getCompatibilityKey(), newRelation);
    return true;
  }

  private void put(Object pKey, VariableRelation<ConstantType> pValue) {
    if (pValue == null) {
      backingMap.remove(pKey);
    } else {
      backingMap.put(pKey, pValue);
    }
  }

  @Override
  public boolean remove(Object pO) {
    if (pO instanceof VariableRelation<?>) {
      return backingMap.remove(((VariableRelation<?>) pO).getCompatibilityKey()) != null;
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> pC) {
    return backingMap.values().containsAll(pC);
  }

  @Override
  public boolean addAll(Collection<? extends VariableRelation<ConstantType>> pC) {
    boolean added = false;
    for (VariableRelation<ConstantType> relation : pC) {
      added |= add(relation);
    }
    return added;
  }

  @Override
  public boolean retainAll(Collection<?> pC) {
   return backingMap.values().retainAll(pC);
  }

  @Override
  public boolean removeAll(Collection<?> pC) {
    return backingMap.values().removeAll(pC);
  }

  @Override
  public void clear() {
    backingMap.clear();
  }

  @Override
  public String toString() {
    return backingMap.values().toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof VariableRelationSet<?>) {
      return backingMap.equals(((VariableRelationSet<?>) pO).backingMap);
    }
    if (pO instanceof Set<?>) {
      Set<?> other = (Set<?>) pO;
      return containsAll(other) && other.containsAll(this);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return backingMap.hashCode();
  }

}
