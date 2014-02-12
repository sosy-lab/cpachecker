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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.Location;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.Struct;
import org.sosy_lab.cpachecker.cpa.pointer2.util.Union;
import org.sosy_lab.cpachecker.cpa.pointer2.util.Variable;

/**
 * Instances of this class are pointer states that are used as abstract elements
 * in the pointer CPA.
 */
public class PointerState implements AbstractState {

  /**
   * The initial empty pointer state.
   */
  public static final PointerState INITIAL_STATE = new PointerState();

  /**
   * The points-to map of the state.
   */
  private final PersistentSortedMap<Location, LocationSet> pointsToMap;

  /**
   * Creates a new pointer state with an empty initial points-to map.
    */
  private PointerState() {
    pointsToMap = PathCopyingPersistentTreeMap.<Location, LocationSet>of();
  }

  /**
   * Creates a new pointer state from the given persistent points-to map.
   *
   * @param pPointsToMap the points-to map of this state.
   */
  private PointerState(PersistentSortedMap<Location, LocationSet> pPointsToMap) {
    this.pointsToMap = pPointsToMap;
  }

  /**
   * Gets a pointer state representing the points to information of this state
   * combined with the information that the first given identifier points to the
   * second given identifier.
   *
   * @param pSource the first identifier.
   * @param pTarget the second identifier.
   * @return the pointer state
   */
  public PointerState addPointsToInformation(Location pSource, Location pTarget) {
    LocationSet previousPointsToSet = getPointsToSet(pSource);
    LocationSet newPointsToSet = previousPointsToSet.addElement(pTarget);
    return new PointerState(pointsToMap.putAndCopy(pSource, newPointsToSet));
  }

  /**
   * Gets a pointer state representing the points to information of this state
   * combined with the information that the first given identifier points to the
   * given target identifiers.
   *
   * @param pSource the first identifier.
   * @param pTargets the target identifiers.
   * @return the pointer state
   */
  public PointerState addPointsToInformation(Location pSource, Iterable<Location> pTargets) {
    LocationSet previousPointsToSet = getPointsToSet(pSource);
    LocationSet newPointsToSet = previousPointsToSet.addElements(pTargets);
    return new PointerState(pointsToMap.putAndCopy(pSource, newPointsToSet));
  }

  /**
   * Gets a pointer state representing the points to information of this state
   * combined with the information that the first given identifier points to the
   * given target identifiers.
   *
   * @param pSource the first identifier.
   * @param pTargets the target identifiers.
   * @return the pointer state
   */
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

  /**
   * Gets the points-to set mapped to the given identifier.
   *
   * @param pSource the identifier pointing to the points-to set in question.
   * @return the points-to set of the given identifier.
   */
  public LocationSet getPointsToSet(Location pSource) {
    LocationSet result = this.pointsToMap.get(pSource);
    if (result == null) {
      return LocationSet.BOT;
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

  /**
   * Checks whether or not the first identifier is known to point to the second
   * identifier.
   *
   * @param pSource
   * @param pTarget
   * @return <code>true</code> if the first identifier definitely points to the
   * second identifier, <code>false</code> if it might point to it or is known
   * not to point to it.
   */
  public boolean definitelyPointsTo(Location pSource, Location pTarget) {
    return pointsTo(pSource, pTarget) == true;
  }

  /**
   * Checks whether or not the first identifier is known to not point to the
   * second identifier.
   *
   * @param pSource
   * @param pTarget
   * @return <code>true</code> if the first identifier definitely does not
   * points to the second identifier, <code>false</code> if it might point to
   * it or is known to point to it.
   */
  public boolean definitelyNotPointsTo(Location pSource, Location pTarget) {
    return pointsTo(pSource, pTarget) == false;
  }

  /**
   * Checks whether or not the first identifier is may point to the second
   * identifier.
   *
   * @param pSource
   * @param pTarget
   * @return <code>true</code> if the first identifier definitely points to the
   * second identifier or might point to it, <code>false</code> if it is known
   * not to point to it.
   */
  public boolean mayPointTo(Location pSource, Location pTarget) {
    return pointsTo(pSource, pTarget) != false;
  }

  /**
   * Gets the identifier for the given global variable.
   *
   * @param pVariableName the name of the global variable.
   * @return the identifier for the given global variable.
   */
  public Location getVariableAsLocation(String pVariableName) {
    return new Variable(pVariableName);
  }

  /**
   * Gets the identifier for the given local variable.
   *
   * @param pVariableName the name of the function of the variable.
   * @param pVariableName the name of the local variable.
   * @return the identifier for the given local variable.
   */
  public Location getVariableAsLocation(String pFuntionName, String pVariableName) {
    return new Variable(pFuntionName + "::" + pVariableName);
  }

  /**
   * Gets the identifier for the given global struct instance.
   *
   * @param pTypeName the name of the struct type.
   * @param pInstanceName the name of the instance.
   * @return the identifier for the given global struct instance.
   */
  public Location getStructAsLocation(String pTypeName, String pInstanceName) {
    return new Struct(pTypeName);
  }

  /**
   * Gets the identifier for the given local struct instance.
   *
   * @param pTypeName the name of the struct type.
   * @param pInstanceFunctionName the name of the function.
   * @param pInstanceName the name of the instance.
   * @return the identifier for the given local struct instance.
   */
  public Location getStructAsLocation(String pTypeName, String pInstanceFunctionName, String pInstanceName) {
    return new Struct(pTypeName);
  }

  /**
   * Gets the identifier for the given global union instance.
   *
   * @param pTypeName the name of the union type.
   * @param pInstanceName the name of the instance.
   * @return the identifier for the given global union instance.
   */
  public Location getUnionAsLocation(String pTypeName, String pInstanceName) {
    return new Union(pTypeName);
  }

  /**
   * Gets the identifier for the given local union instance.
   *
   * @param pTypeName the name of the union type.
   * @param pInstanceFunctionName the name of the function.
   * @param pInstanceName the name of the instance.
   * @return the identifier for the given local union instance.
   */
  public Location getUnionAsLocation(String pTypeName, String pInstanceFunctionName, String pInstanceName) {
    return new Union(pTypeName);
  }

  /**
   * Gets all locations known to the state.
   *
   * @return all locations known to the state.
   */
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

  /**
   * Gets the points-to map of this state.
   *
   * @return the points-to map of this state.
   */
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
