/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableSet;

/**
 * State for Symbolic Execution Analysis.
 *
 * <p>This state contains a mapping of tracked variables and their corresponding symbolic value.
 * </p>
 */
public class ConstraintsState implements LatticeAbstractState<ConstraintsState> {

  /**
   * Stores identifiers and their corresponding constraints
   */
  private Set<Constraint> constraints;

  /**
   * Stores whether this states constraints are solvable.
   */
  private boolean isSolvable = true;

  /**
   * Creates a new <code>ConstraintsState</code> object with the given constraints.
   *
   * @param pConstraints the constraints to use for the newly created <code>ConstraintsState</code> object
   */
  public ConstraintsState(Set<Constraint> pConstraints) {
    constraints = new HashSet<>(pConstraints);
  }

  /**
   * Creates a new, initial <code>ConstraintsState</code> object.
   */
  public ConstraintsState() {
    constraints = new HashSet<>();
  }

  /**
   * Returns a new copy of the given <code>ConstraintsState</code> object.
   *
   * @param pState the state to copy
   * @return a new copy of the given <code>ConstraintsState</code> object
   */
  public static ConstraintsState copyOf(ConstraintsState pState) {
    return new ConstraintsState(new HashSet<>(pState.constraints));
  }

  /**
   * Returns the constraints stored in this state.
   *
   * <p>A new set is returned, so changing the returned set will not change the constraints in this state.</p>
   *
   * @return the constraints stored in this state
   */
  public Set<Constraint> getConstraints() {
    return ImmutableSet.copyOf(constraints);
  }

  @Override
  public ConstraintsState join(ConstraintsState other) throws CPAException {
    // we currently use merge^sep
    throw new UnsupportedOperationException();
  }

  /**
   * Returns whether this state is less or equal than another given state.
   *
   * @param other the other state to check against
   * @return <code>true</code> if this state is less or equal than the given state, <code>false</code> otherwise
   */
  @Override
  public boolean isLessOrEqual(ConstraintsState other) {
    boolean lessOrEqual = false;

    for (Constraint otherConstraint : other.constraints) {
      for (Constraint currConstraint : constraints) {
        if (otherConstraint.equals(currConstraint) || otherConstraint.includes(currConstraint)) {
          lessOrEqual = true;
          break;
        }
      }

      if (!lessOrEqual) {
        break;
      }

      lessOrEqual = false;
    }

    return lessOrEqual;
  }

  /**
   * Adds the given {@link Constraint} to this state.
   *
   * @param pConstraint the <code>Constraint</code> to add
   */
  public void addConstraint(Constraint pConstraint) {
    constraints.add(pConstraint);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");

    for (Constraint currConstraint : constraints) {
      sb.append(" <");
      sb.append(currConstraint);
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(constraints.size()).toString();
  }
}
