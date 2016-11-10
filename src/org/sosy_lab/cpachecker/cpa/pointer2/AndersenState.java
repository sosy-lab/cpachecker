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
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;

public class AndersenState extends PointerState {

  /**
   * The initial empty pointer state.
   */
  public static final PointerState INITIAL_STATE = new AndersenState();

  public AndersenState() {
    super();
  }

  /**
   * Creates a new pointer state from the given persistent points-to map.
   *
   * @param pPointsToMap the points-to map of this state.
   */
  private AndersenState(PersistentSortedMap<LocationSet, LocationSet> pPointsToMap) {
    super(pPointsToMap);
  }

  @Override
  public PointerState addPointsToInformation(LocationSet pSource, LocationSet pTargets) {
    if (pTargets.isBot()) {
      return this;
    }
    LocationSet newLocs;
    if (pTargets.isTop()) {
      newLocs = LocationSetTop.INSTANCE;
    } else {
      newLocs = getPointsToSet(pSource).addElements(pTargets);
    }
    return new AndersenState(getPointsToMap().putAndCopy(pSource, newLocs));
  }
}
