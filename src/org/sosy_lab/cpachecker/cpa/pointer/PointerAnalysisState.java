// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer;

import java.util.Objects;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.pointer.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSetTop;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PointerAnalysisState implements LatticeAbstractState<PointerAnalysisState> {

  private final boolean isBottom;
  public static final PointerAnalysisState BOTTOM_STATE = new PointerAnalysisState(true);
  private final PersistentMap<MemoryLocation, LocationSet> pointsToMap;

  public PointerAnalysisState(boolean pBottom) {
    isBottom = pBottom;
    pointsToMap = PathCopyingPersistentTreeMap.of();
  }

  public PointerAnalysisState() {
    isBottom = true;
    pointsToMap = PathCopyingPersistentTreeMap.of();
  }

  public PointerAnalysisState(PersistentMap<MemoryLocation, LocationSet> pPointsToMap) {
    isBottom = false;
    pointsToMap = pPointsToMap;
  }

  public PersistentMap<MemoryLocation, LocationSet> getPointsToMap() {
    return pointsToMap;
  }

  public LocationSet getPointsToSet(MemoryLocation pSource) {
    LocationSet pointsToSet = pointsToMap.get(pSource);
    if (pointsToSet == null) {
      return LocationSetTop.INSTANCE;
    }
    if (pointsToSet.isNull()) {
      return ExplicitLocationSet.fromNull();
    }
    return pointsToSet;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof PointerAnalysisState other
        && isBottom == other.isBottom
        && pointsToMap.entrySet().equals(other.pointsToMap.entrySet());
  }

  @Override
  public int hashCode() {
    return Objects.hash(isBottom, pointsToMap.entrySet());
  }

  @Override
  public String toString() {
    String mapString =
        pointsToMap.entrySet().stream()
            .limit(10)
            .map(e -> e.getKey() + " -> " + e.getValue())
            .collect(Collectors.joining(", "));
    return "PointerAnalysisState{"
        + "pointsToMap={"
        + mapString
        + (pointsToMap.size() > 10 ? ", ..." : "")
        + "}}";
  }

  public boolean isBottom() {
    return isBottom;
  }

  @Override
  public boolean isLessOrEqual(PointerAnalysisState pOtherState)
      throws CPAException, InterruptedException {

    if (this == pOtherState) {
      return true;
    }

    if (isBottom()) {
      return true;
    }

    if (pOtherState.isBottom()) {
      return false;
    }

    for (MemoryLocation source : pointsToMap.keySet()) {
      LocationSet thisTargets = getPointsToSet(source);
      LocationSet otherTargets = pOtherState.getPointsToSet(source);

      if (thisTargets.isTop() && !otherTargets.isTop()) {
        return false;
      }
      if (!otherTargets.containsAll(thisTargets)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public PointerAnalysisState join(PointerAnalysisState pOtherState)
      throws CPAException, InterruptedException {
    if (isBottom()) {
      return pOtherState;
    }
    if (pOtherState.isBottom()) {
      return this;
    }
    if (this.equals(pOtherState)) {
      return this;
    }

    PersistentMap<MemoryLocation, LocationSet> joinedMap = pointsToMap;

    for (MemoryLocation source : pointsToMap.keySet()) {
      LocationSet thisTargets = getPointsToSet(source);
      LocationSet otherTargets = pOtherState.getPointsToSet(source);

      LocationSet joinedTargets;
      if (thisTargets.isTop() || otherTargets.isTop()) {
        joinedMap = joinedMap.removeAndCopy(source);
      } else {
        joinedTargets = thisTargets.addElements(otherTargets);
        joinedMap = joinedMap.putAndCopy(source, joinedTargets);
      }
    }

    for (MemoryLocation source : pOtherState.pointsToMap.keySet()) {
      if (!pointsToMap.containsKey(source)) {
        LocationSet otherTargets = pOtherState.getPointsToSet(source);
        joinedMap = joinedMap.putAndCopy(source, otherTargets);
      }
    }
    if (joinedMap.equals(this.pointsToMap)) {
      return this;
    }
    if (joinedMap.equals(pOtherState.pointsToMap)) {
      return pOtherState;
    }

    return new PointerAnalysisState(joinedMap);
  }
}
