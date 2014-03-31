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
package org.sosy_lab.cpachecker.cpa.bdd;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.base.Joiner;

public class BDDState implements AbstractQueryableState {

  private Region currentState;
  private final NamedRegionManager manager;
  private final BitvectorManager bvmgr;

  public BDDState(NamedRegionManager mgr, BitvectorManager bvmgr, Region state) {
    this.currentState = state;
    this.manager = mgr;
    this.bvmgr = bvmgr;
  }

  public Region getRegion() {
    return currentState;
  }

  public boolean isLessOrEqual(BDDState other) throws InterruptedException {
    return manager.entails(this.currentState, other.currentState);
  }

  public BDDState join(BDDState other) {
     Region result = manager.makeOr(this.currentState, other.currentState);

    // FIRST check the other element
    if (result.equals(other.currentState)) {
      return other;

      // THEN check this element
    } else if (result.equals(this.currentState)) {
      return this;

    } else {
      return new BDDState(manager, bvmgr, result);
    }
  }

  @Override
  public String toString() {
    return //manager.dumpRegion(currentState) + "\n" +
        manager.regionToDot(currentState);
  }

  public String toCompactString() {
    return "";//manager.dumpRegion(currentState);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BDDState) {
      BDDState other = (BDDState) o;
      return this.currentState.equals(other.currentState);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return currentState.hashCode();
  }

  @Override
  public String getCPAName() {
    return "BDDCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    throw new InvalidQueryException("BDDCPA Element cannot check anything");
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals("VALUES")) {
      return manager.dumpRegion(this.currentState);
    } else if (pProperty.equals("VARSET")) {
      return "(" + Joiner.on(", ").join(manager.getPredicates()) + ")";
    } else if (pProperty.equals("VARSETSIZE")) {
      return manager.getPredicates().size();
    } else {
      throw new InvalidQueryException("BDDCPA Element can only return the current values (\"VALUES\")");
    }
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    throw new InvalidQueryException("BDDCPA Element cannot be modified");
  }

  /** this.state = this.state.and(pConstraint);
   */
  public void addConstraintToState(Region pConstraint) {
    currentState = manager.makeAnd(currentState, pConstraint);
  }

  /** This function adds the constraint to the state. */
  public BDDState addConstraint(Region constraint) {
    return new BDDState(manager, bvmgr, manager.makeAnd(currentState, constraint));
  }

  /** This function adds the equality of left and right side to the state.
   * If left or right side is null, the state is returned unchanged. */
  public BDDState addAssignment(@Nullable Region[] leftSide, @Nullable Region[] rightSide) {
    return addAssignment(leftSide, rightSide, false);
  }


  /** This function adds the equality of left and right side to the current state.
   * If left or right side is null, the state is returned unchanged.
   *
   * @param addIncreasing order of iteration, might cause better performance */
  private BDDState addAssignment(@Nullable Region[] leftSide, @Nullable Region[] rightSide, final boolean addIncreasing) {
    if (leftSide == null || rightSide == null) {
      return this;
    } else {
      assert leftSide.length == rightSide.length : "left side and right side should have equal length: "
              + leftSide.length + " != " + rightSide.length;
      final Region[] assignRegions = bvmgr.makeBinaryEqual(leftSide, rightSide);

      Region result;

      if (addIncreasing) {
        result = assignRegions[0];
        for (int i = 1; i < assignRegions.length; i++) {
          result = manager.makeAnd(result, assignRegions[i]);
        }
      } else {
        result = assignRegions[assignRegions.length - 1];
        for (int i = assignRegions.length - 2; i >= 0; i--) {
          result = manager.makeAnd(result, assignRegions[i]);
        }
      }

      result = manager.makeAnd(currentState, result);

      return new BDDState(manager, bvmgr, result);
    }
  }

  /** This function removes all information about the Regions from current state. */
  public BDDState forget(@Nullable final Region... toForget) {
    if (toForget == null) {
      return this;
    }
    return new BDDState(manager, bvmgr, manager.makeExists(currentState, toForget));
  }
}