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
package org.sosy_lab.cpachecker.cpa.andersen;

import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.andersen.util.BaseConstraint;
import org.sosy_lab.cpachecker.cpa.andersen.util.ComplexConstraint;
import org.sosy_lab.cpachecker.cpa.andersen.util.ConstraintSystem;
import org.sosy_lab.cpachecker.cpa.andersen.util.SimpleConstraint;

public class AndersenState implements AbstractState {

  /**
   * The local constraint system.
   */
  private final ConstraintSystem localConstraintSystem;

  public AndersenState() {
    this(null);
  }

  public AndersenState(ConstraintSystem pLocalConstraintSystem) {
    this.localConstraintSystem = pLocalConstraintSystem == null ? new ConstraintSystem() : pLocalConstraintSystem;
  }

  /**
   * Adds a (new) {@link BaseConstraint} returns the result.
   * This instance is not modified by the operation.
   *
   * @param pConstr {@link BaseConstraint} that should be added.
   */
  AndersenState addConstraint(BaseConstraint pConstr) {
    return new AndersenState(this.localConstraintSystem.addConstraint(pConstr));
  }

  /**
   * Adds a (new) {@link SimpleConstraint} and returns the result.
   * This instance is not modified by the operation.
   *
   * @param pConstr {@link SimpleConstraint} that should be added.
   */
  AndersenState addConstraint(SimpleConstraint pConstr) {
    return new AndersenState(this.localConstraintSystem.addConstraint(pConstr));
  }

  /**
   * Adds a (new) {@link ComplexConstraint} and returns the result.
   * This instance is not modified by the operation.
   *
   * @param pConstr {@link ComplexConstraint} that should be added.
   */
  AndersenState addConstraint(ComplexConstraint pConstr) {
    return new AndersenState(this.localConstraintSystem.addConstraint(pConstr));
  }

  /**
   * Computes and returns the points-to sets for the local constraint system.
   *
   * @return points-to sets for the local constraint system.
   */
  public Map<String, String[]> getLocalPointsToSets() {
    return this.localConstraintSystem.getPointsToSets();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof AndersenState) {
      AndersenState other = (AndersenState) pO;
      return localConstraintSystem.equals(other.localConstraintSystem);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.localConstraintSystem.hashCode();
  }

  @Override
  public String toString() {
    return this.localConstraintSystem.toString();
  }

  public boolean isLessOrEqual(AndersenState pReachedState) {
    return this == pReachedState || (this.localConstraintSystem.getBaseConstraints().containsAll(pReachedState.localConstraintSystem.getBaseConstraints())
        && this.localConstraintSystem.getSimpleConstraints().containsAll(pReachedState.localConstraintSystem.getSimpleConstraints())
        && this.localConstraintSystem.getComplexConstraints().containsAll(pReachedState.localConstraintSystem.getComplexConstraints()));
  }

  public AndersenState join(AndersenState pReachedState) {
    if (isLessOrEqual(pReachedState)) {
      return pReachedState;
    }
    if (pReachedState.isLessOrEqual(this)) {
      return this;
    }
    return new AndersenState(this.localConstraintSystem.join(pReachedState.localConstraintSystem));
  }
}
