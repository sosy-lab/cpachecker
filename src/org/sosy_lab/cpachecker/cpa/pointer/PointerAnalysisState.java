// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer;

import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.pointer.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSetTop;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PointerAnalysisState implements LatticeAbstractState<PointerAnalysisState> {

  private final boolean isBottom;
  public static final PointerAnalysisState BOTTOM_STATE = new PointerAnalysisState(true);
  private final PersistentMap<LocationSet, LocationSet> pointsToMap;

  public PointerAnalysisState(boolean pBottom) {
    isBottom = pBottom;
    pointsToMap = PathCopyingPersistentTreeMap.of();
  }

  public PointerAnalysisState() {
    isBottom = true;
    pointsToMap = PathCopyingPersistentTreeMap.of();
  }

  public PointerAnalysisState(PersistentMap<LocationSet, LocationSet> pPointsToMap) {
    isBottom = false;
    pointsToMap = pPointsToMap;
  }

  public PersistentMap<LocationSet, LocationSet> getPointsToMap() {
    return pointsToMap;
  }

  public LocationSet getPointsToSet(LocationSet pSource) {
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
        && (isBottom || pointsToMap.equals(other.pointsToMap));
  }

  @Override
  public int hashCode() {
    if (isBottom) {
      return 0;
    }

    int hash = 31;
    hash = 31 * hash + Boolean.hashCode(isBottom);
    hash = 31 * hash + pointsToMap.hashCode();
    return hash;
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

  public PointerAnalysisState addPointsToInformation(LocationSet pSource, LocationSet pTargets) {
    if (pTargets.isBot()) {
      return this;
    }
    LocationSet updatedPointsToSet;
    if (pTargets.isTop()) {
      updatedPointsToSet = LocationSetTop.INSTANCE;
    } else {
      updatedPointsToSet = getPointsToSet(pSource).addElements(pTargets);
    }
    return new PointerAnalysisState(getPointsToMap().putAndCopy(pSource, updatedPointsToSet));
  }

  public boolean isBottom() {
    return isBottom;
  }

  @Override
  public boolean isLessOrEqual(PointerAnalysisState pOtherState)
      throws CPAException, InterruptedException {

    if (isBottom()) {
      return true;
    }

    if (pOtherState.isBottom()) {
      return false;
    }

    for (LocationSet source : pointsToMap.keySet()) {
      LocationSet thisTargets = getPointsToSet(source);
      LocationSet otherTargets = pOtherState.getPointsToSet(source);

      if (!otherTargets.isTop() && !otherTargets.containsAll(thisTargets)) {
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

    PersistentMap<LocationSet, LocationSet> joinedMap = pointsToMap;

    for (LocationSet source : pointsToMap.keySet()) {
      LocationSet thisTargets = getPointsToSet(source);
      LocationSet otherTargets = pOtherState.getPointsToSet(source);

      LocationSet joinedTargets;
      if (thisTargets.isTop() || otherTargets.isTop()) {
        joinedTargets = LocationSetTop.INSTANCE;
      } else {
        joinedTargets = thisTargets.addElements(otherTargets);
      }

      joinedMap = joinedMap.putAndCopy(source, joinedTargets);
    }

    for (LocationSet source : pOtherState.pointsToMap.keySet()) {
      if (!pointsToMap.containsKey(source)) {
        LocationSet otherTargets = pOtherState.getPointsToSet(source);
        joinedMap = joinedMap.putAndCopy(source, otherTargets);
      }
    }

    return new PointerAnalysisState(joinedMap);
  }
}
