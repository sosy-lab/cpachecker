// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.andersen;

import com.google.common.collect.ImmutableListMultimap;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.andersen.util.BaseConstraint;
import org.sosy_lab.cpachecker.cpa.andersen.util.ComplexConstraint;
import org.sosy_lab.cpachecker.cpa.andersen.util.ConstraintSystem;
import org.sosy_lab.cpachecker.cpa.andersen.util.SimpleConstraint;

public class AndersenState implements LatticeAbstractState<AndersenState> {

  /** The local constraint system. */
  private final ConstraintSystem localConstraintSystem;

  public AndersenState() {
    this(null);
  }

  public AndersenState(ConstraintSystem pLocalConstraintSystem) {
    localConstraintSystem =
        pLocalConstraintSystem == null ? new ConstraintSystem() : pLocalConstraintSystem;
  }

  /**
   * Adds a (new) {@link BaseConstraint} returns the result. This instance is not modified by the
   * operation.
   *
   * @param pConstr {@link BaseConstraint} that should be added.
   */
  AndersenState addConstraint(BaseConstraint pConstr) {
    return new AndersenState(localConstraintSystem.addConstraint(pConstr));
  }

  /**
   * Adds a (new) {@link SimpleConstraint} and returns the result. This instance is not modified by
   * the operation.
   *
   * @param pConstr {@link SimpleConstraint} that should be added.
   */
  AndersenState addConstraint(SimpleConstraint pConstr) {
    return new AndersenState(localConstraintSystem.addConstraint(pConstr));
  }

  /**
   * Adds a (new) {@link ComplexConstraint} and returns the result. This instance is not modified by
   * the operation.
   *
   * @param pConstr {@link ComplexConstraint} that should be added.
   */
  AndersenState addConstraint(ComplexConstraint pConstr) {
    return new AndersenState(localConstraintSystem.addConstraint(pConstr));
  }

  /**
   * Computes and returns the points-to sets for the local constraint system.
   *
   * @return points-to sets for the local constraint system.
   */
  public ImmutableListMultimap<String, String> getLocalPointsToSets() {
    return localConstraintSystem.getPointsToSets();
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
    return localConstraintSystem.hashCode();
  }

  @Override
  public String toString() {
    return localConstraintSystem.toString();
  }

  @Override
  public boolean isLessOrEqual(AndersenState pReachedState) {
    return Objects.equals(this, pReachedState)
        || (localConstraintSystem
                .getBaseConstraints()
                .containsAll(pReachedState.localConstraintSystem.getBaseConstraints())
            && localConstraintSystem
                .getSimpleConstraints()
                .containsAll(pReachedState.localConstraintSystem.getSimpleConstraints())
            && localConstraintSystem
                .getComplexConstraints()
                .containsAll(pReachedState.localConstraintSystem.getComplexConstraints()));
  }

  @Override
  public AndersenState join(AndersenState pReachedState) {
    if (isLessOrEqual(pReachedState)) {
      return pReachedState;
    }
    if (pReachedState.isLessOrEqual(this)) {
      return this;
    }
    return new AndersenState(localConstraintSystem.join(pReachedState.localConstraintSystem));
  }
}
