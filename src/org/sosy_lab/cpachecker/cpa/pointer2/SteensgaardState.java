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
package org.sosy_lab.cpachecker.cpa.pointer2;

import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;

public class SteensgaardState extends PointerState {

  /**
   * The initial empty pointer state.
   */
  public static final PointerState INITIAL_STATE = new SteensgaardState();

  private SteensgaardState() {
    super();
  }

  /**
   * Creates a new pointer state from the given persistent points-to map.
   *
   * @param pPointsToMap the points-to map of this state.
   */
  private SteensgaardState(PersistentSortedMap<LocationSet, LocationSet> pPointsToMap) {
    super(pPointsToMap);
  }

  /**
   * Gets a pointer state representing the points-to information of this state
   * combined with the information that the first given identifier points to the
   * given target identifiers.
   *
   * @param pSource the first identifier.
   * @param pTargets the target identifiers.
   * @return the pointer state.
   */
  @Override
  public PointerState addPointsToInformation(LocationSet pSource, LocationSet pTargets) {
    if (pTargets.isBot()) {
      return this;
    }
    PersistentSortedMap<LocationSet, LocationSet> newLocations;
    if (pTargets.isTop()) {
      newLocations = getPointsToMap().putAndCopy(pSource, LocationSetTop.INSTANCE);
    } else {
      LocationSet previousPointsToSet = getPointsToSet(pSource);
      if (previousPointsToSet.isBot()) {
        newLocations = getPointsToMap().putAndCopy(pSource, pTargets);
      } else {
        newLocations = join(getPointsToMap(), previousPointsToSet, pTargets);
      }
    }
    return new SteensgaardState(newLocations);
  }

  private PersistentSortedMap<LocationSet, LocationSet> join(
      PersistentSortedMap<LocationSet, LocationSet> currentMap,
      LocationSet pLhs,
      LocationSet pRhs) {
    if (pLhs.equals(pRhs)) {
      return currentMap;
    }
    LocationSet pLhsNext = getPointsToSet(pLhs);
    LocationSet pRhsNext = getPointsToSet(pRhs);
    currentMap = unify(currentMap, pLhs, pRhs);
    if (!(pRhsNext instanceof LocationSetBot || pLhsNext instanceof LocationSetBot)
        && (!pRhsNext.containsAll(pLhsNext) && pLhs.containsAll(pRhsNext))) {
      currentMap = join(currentMap, pLhsNext, pRhsNext);
    }
    return currentMap;
  }

  /**
   * Updates the map accordingly to the new merged nodes.
   */
  private static PersistentSortedMap<LocationSet, LocationSet> unify(PersistentSortedMap<LocationSet, LocationSet> currentMap, LocationSet pMergeLeft, LocationSet pMergeRight) {
    LocationSet resultLocation = pMergeLeft.addElements(pMergeRight);
    LocationSet referencedValues = new LocationSetBot();
    for (LocationSet pKey : currentMap.keySet()) {
      if ((currentMap.get(pKey).containsAll(pMergeLeft) || currentMap.get(pKey).containsAll(pMergeRight))
          && !pKey.containsAll(resultLocation)) {
        currentMap = currentMap.removeAndCopy(pKey);
        currentMap = currentMap.putAndCopy(pKey, resultLocation);
      }
      if ((pKey.containsAll(pMergeLeft) || pKey.containsAll(pMergeRight))
          && !pKey.containsAll(resultLocation)) {
        referencedValues = referencedValues.addElements(currentMap.get(pKey));
        currentMap = currentMap.removeAndCopy(pKey);
      }
    }
    if (referencedValues instanceof ExplicitLocationSet
        || referencedValues instanceof LocationSetTop) {
      currentMap = currentMap.putAndCopy(resultLocation, referencedValues);
    }
    return currentMap;
  }
}
