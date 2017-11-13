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
package org.sosy_lab.cpachecker.cpa.pointer2;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Instances of this class are pointer states that are used as abstract elements
 * in the pointer CPA.
 */
public abstract class PointerState implements AbstractState {


  /**
   * The points-to map of the state.
   */
  private final PersistentSortedMap<LocationSet, LocationSet> pointsToMap;

  /**
   * Creates a new pointer state with an empty initial points-to map.
    */
  protected PointerState() {
    pointsToMap = PathCopyingPersistentTreeMap.<LocationSet, LocationSet>of();
  }

  /**
   * Creates a new pointer state from the given persistent points-to map.
   *
   * @param pPointsToMap the points-to map of this state.
   */
  protected PointerState(PersistentSortedMap<LocationSet, LocationSet> pPointsToMap) {
    this.pointsToMap = pPointsToMap;
  }

  /**
   * Gets a pointer state representing the points to information of this state
   * combined with the information that the first given identifier points to the
   * second given identifier.
   *
   * @param pSource the first identifier.
   * @param pTarget the second identifier.
   * @return the pointer state.
   */
  public abstract PointerState addPointsToInformation(LocationSet pSource, LocationSet pTarget);


  /**
   * Gets the points-to set mapped to the given identifier.
   *
   * @param pSource the identifier pointing to the points-to set in question.
   * @return the points-to set of the given identifier.
   */
  public LocationSet getPointsToSet(LocationSet pSource) {
    LocationSet result = this.pointsToMap.get(pSource);
    if (result == null) {
      return LocationSetBot.INSTANCE;
    }
    return result;
  }

  /**
   * Checks whether or not the first identifier points to the second identifier.
   *
   * @param pSource the first identifier.
   * @param pTarget the second identifier.
   * @return <code>true</code> if the first identifier definitely points to the
   * second identifier, <code>false</code> if it definitely does not point to
   * the second identifier and <code>null</code> if it might point to it.
   */
  @Nullable
  public Boolean pointsTo(LocationSet pSource, LocationSet pTarget) {
    LocationSet pointsToSet = getPointsToSet(pSource);
    if (pointsToSet.equals(LocationSetBot.INSTANCE)) {
      return false;
    }
    return pointsToMap.get(pSource).equals(pTarget);
  }

  /**
   * Checks whether or not the first identifier is known to point to the second
   * identifier.
   *
   * @return <code>true</code> if the first identifier definitely points to the
   * second identifier, <code>false</code> if it might point to it or is known
   * not to point to it.
   */
  public boolean definitelyPointsTo(LocationSet pSource, LocationSet pTarget) {
    return Boolean.TRUE.equals(pointsTo(pSource, pTarget));
  }

  /**
   * Checks whether or not the first identifier is known to not point to the
   * second identifier.
   *
   * @return <code>true</code> if the first identifier definitely does not
   * points to the second identifier, <code>false</code> if it might point to
   * it or is known to point to it.
   */
  public boolean definitelyNotPointsTo(LocationSet pSource, LocationSet pTarget) {
    return Boolean.FALSE.equals(pointsTo(pSource, pTarget));
  }

  /**
   * Checks whether or not the first identifier is may point to the second
   * identifier.
   *
   * @return <code>true</code> if the first identifier definitely points to the
   * second identifier or might point to it, <code>false</code> if it is known
   * not to point to it.
   */
  public boolean mayPointTo(LocationSet pSource, LocationSet pTarget) {
    return !Boolean.FALSE.equals(pointsTo(pSource, pTarget));
  }

  /**
   * Gets all locations known to the state.
   *
   * @return all locations known to the state.
   */
  public Set<MemoryLocation> getKnownLocations() {
    ImmutableSet.Builder<MemoryLocation> builder = ImmutableSet.builder();
    for (LocationSet keys : pointsToMap.keySet()) {
      if (keys instanceof ExplicitLocationSet) {
        ExplicitLocationSet explicitKeys = (ExplicitLocationSet) keys;
        builder.addAll(explicitKeys.getExplicitLocations());
      }
    }
    for (LocationSet values : pointsToMap.values()) {
      if (values instanceof ExplicitLocationSet) {
        ExplicitLocationSet explicitValues = (ExplicitLocationSet) values;
        builder.addAll(explicitValues.getExplicitLocations());
      }
    }
    return builder.build();
  }

  /**
   * Gets the points-to map of this state.
   *
   * @return the points-to map of this state.
   */
  public PersistentSortedMap<LocationSet, LocationSet> getPointsToMap() {
    return pointsToMap;
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

  public Set<MemoryLocation> toExplicitSet(LocationSet pLocationSet) {
    if (pLocationSet.isBot()) {
      return Collections.emptySet();
    }
    if (pLocationSet.isTop()) {
      return getKnownLocations();
    }
    return ((ExplicitLocationSet) pLocationSet).getExplicitLocations();
  }

}
