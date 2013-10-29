/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.Location;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;


public class PointerState implements AbstractState {

  private final PersistentSortedMap<Location, LocationSet> pointsToMap;

  public PointerState() {
    pointsToMap = PathCopyingPersistentTreeMap.<Location, LocationSet>of();
  }

  private PointerState(PersistentSortedMap<Location, LocationSet> pPointsToMap) {
    this.pointsToMap = pPointsToMap;
  }

  public PointerState addPointsToInformation(Location pSource, Location pTarget) {
    LocationSet previousPointsToSet = getPointsToSet(pSource);
    LocationSet newPointsToSet = previousPointsToSet.addElement(pTarget);
    return new PointerState(pointsToMap.putAndCopy(pSource, newPointsToSet));
  }

  public PointerState addPointsToInformation(Location pSource, Iterable<Location> pTargets) {
    LocationSet previousPointsToSet = getPointsToSet(pSource);
    LocationSet newPointsToSet = previousPointsToSet.addElements(pTargets);
    return new PointerState(pointsToMap.putAndCopy(pSource, newPointsToSet));
  }

  public PointerState addPointsToInformation(Location pSource, LocationSet pTargets) {
    if (pTargets.isBot()) {
      return this;
    }
    if (pTargets.isTop()) {
      return new PointerState(pointsToMap.putAndCopy(pSource, LocationSet.TOP));
    }
    LocationSet previousPointsToSet = getPointsToSet(pSource);
    return new PointerState(pointsToMap.putAndCopy(pSource, previousPointsToSet.addElements(pTargets)));
  }

  public LocationSet getPointsToSet(Location pSource) {
    LocationSet result = this.pointsToMap.get(pSource);
    if (result == null) {
      return LocationSet.BOT;
    }
    return result;
  }

  public Boolean pointsTo(Location pSource, Location pTarget) {
    LocationSet pointsToSet = getPointsToSet(pSource);
    if (pointsToSet.equals(LocationSet.BOT)) {
      return false;
    }
    if (pointsToSet instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pointsToSet;
      if (explicitLocationSet.mayPointTo(pTarget)) {
        return explicitLocationSet.getElements().size() == 1 ? true : null;
      } else {
        return false;
      }
    }
    return null;
  }

  public boolean definitelyPointsTo(Location pSource, Location pTarget) {
    return pointsTo(pSource, pTarget) == true;
  }

  public boolean definitelyNotPointsTo(Location pSource, Location pTarget) {
    return pointsTo(pSource, pTarget) == false;
  }

  public boolean mayPointTo(Location pSource, Location pTarget) {
    return pointsTo(pSource, pTarget) != false;
  }

  public Iterable<Location> getKnownLocations() {
    Set<Location> locations = new HashSet<>();
    locations.addAll(pointsToMap.keySet());
    for (LocationSet targetSet : pointsToMap.values()) {
      if (targetSet instanceof ExplicitLocationSet) {
        locations.addAll(((ExplicitLocationSet) targetSet).getElements());
      }
    }
    return locations;
  }

  public Map<Location, LocationSet> getPointsToMap() {
    return Collections.unmodifiableMap(this.pointsToMap);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof PointerState) {
      return pointsToMap.equals(((PointerState) pO).pointsToMap);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return pointsToMap.hashCode();
  }

  @Override
  public String toString() {
    return pointsToMap.toString();
  }

}
