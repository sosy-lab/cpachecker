// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public final class PointerState implements AbstractState {

  private final PointerStatePrecision precision;
  private final PersistentSortedMap<Key, TargetSet> pointsToMap;

  private PointerState(
      PointerStatePrecision pPrecision, PersistentSortedMap<Key, TargetSet> pPointsToMap) {

    checkNotNull(pPrecision);
    checkNotNull(pPointsToMap);

    precision = pPrecision;
    pointsToMap = pPointsToMap;
  }

  public static PointerState empty(PointerStatePrecision pPrecision) {
    return new PointerState(pPrecision, PathCopyingPersistentTreeMap.<Key, TargetSet>of());
  }

  public TargetSet getTargetSet(PointerState.Key pKey) {
    return pointsToMap.getOrDefault(pKey, TargetSetBot.INSTANCE);
  }

  public TargetSet getTargetSet(CType pType, MemorySegment pPointer) {

    TargetSet result = TargetSetTop.INSTANCE;

    for (Key key : precision.keys(pType, pPointer)) {

      TargetSet targetSet = pointsToMap.getOrDefault(key, TargetSetBot.INSTANCE);
      if (targetSet.equals(TargetSetBot.INSTANCE)) {
        return TargetSetBot.INSTANCE;
      }

      if (targetSet instanceof ExplicitTargetSet) {
        if (result instanceof ExplicitTargetSet) {
          if (((ExplicitTargetSet) targetSet).getSize() < ((ExplicitTargetSet) result).getSize()) {
            result = targetSet;
          }
        } else {
          result = targetSet;
        }
      }
    }

    return result;
  }

  public PointerState add(CType pType, MemorySegment pPointer, TargetSet pTargetSet) {

    PersistentSortedMap<Key, TargetSet> newPointsToMap = pointsToMap;

    for (Key key : precision.keys(pType, pPointer)) {
      TargetSet targetSet = pointsToMap.getOrDefault(key, TargetSetBot.INSTANCE);
      TargetSet unionTargetSet = precision.union(key, targetSet, pTargetSet);
      newPointsToMap = newPointsToMap.putAndCopy(key, unionTargetSet);
    }

    return new PointerState(precision, newPointsToMap);
  }

  public PointerState add(CType pType, MemorySegment pPointer, MemorySegment pTarget) {
    return add(pType, pPointer, SingletonTargetSet.of(pTarget));
  }

  public PointerState join(PointerState pOther) {

    PersistentSortedMap<Key, TargetSet> newPointsToMap = pointsToMap;

    for (Map.Entry<Key, TargetSet> pointsToEntry : pOther.pointsToMap.entrySet()) {
      TargetSet targetSet =
          newPointsToMap.getOrDefault(pointsToEntry.getKey(), TargetSetBot.INSTANCE);
      TargetSet unionTargetSet =
          precision.union(pointsToEntry.getKey(), targetSet, pointsToEntry.getValue());
      newPointsToMap = newPointsToMap.putAndCopy(pointsToEntry.getKey(), unionTargetSet);
    }

    return new PointerState(precision, newPointsToMap);
  }

  public boolean isLessOrEqual(PointerState pOther) {

    if (this == pOther) {
      return true;
    }

    for (Map.Entry<Key, TargetSet> pointsToEntry : pointsToMap.entrySet()) {
      TargetSet targetSet =
          pOther.pointsToMap.getOrDefault(pointsToEntry.getKey(), TargetSetBot.INSTANCE);
      if (!targetSet.includes(pointsToEntry.getValue())) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    return pointsToMap.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {

    if (this == pOther) {
      return true;
    }

    if (!(pOther instanceof PointerState)) {
      return false;
    }

    return pointsToMap.equals(((PointerState) pOther).pointsToMap);
  }

  @Override
  public String toString() {
    return pointsToMap.toString();
  }

  public interface Key extends Comparable<Key> {}
}
