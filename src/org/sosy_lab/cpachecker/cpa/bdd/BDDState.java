// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.base.Joiner;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.java_smt.api.SolverException;

import org.checkerframework.checker.nullness.qual.Nullable;

public class BDDState implements AbstractQueryableState,
    LatticeAbstractState<BDDState> {

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

  @Override
  public boolean isLessOrEqual(BDDState other) throws CPAException, InterruptedException {
    try {
      return manager.entails(this.currentState, other.currentState);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure", e);
    }
  }

  @Override
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
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    switch (pProperty) {
      case "VALUES":
        return manager.dumpRegion(this.currentState).toString();
      case "VARSET":
        return "(" + Joiner.on(", ").join(manager.getPredicates()) + ")";
      case "VARSETSIZE":
        return manager.getPredicates().size();
      default:
        throw new InvalidQueryException(
            "BDDCPA Element can only return the current values (\"VALUES\")");
    }
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