// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * A composite state of {@link ValueAnalysisState} and {@link ConstraintsState} that allows to
 * remove and re-add values.
 */
public final class ForgettingCompositeState implements ForgetfulState<ValueAnalysisInformation> {

  private final ValueAnalysisState values;
  private final ConstraintsState constraints;

  public static ForgettingCompositeState getInitialState(MachineModel pMachineModel) {
    return new ForgettingCompositeState(
        new ValueAnalysisState(pMachineModel), new ConstraintsState());
  }

  /**
   * Creates a new state with the given value analysis state and constraints state.
   *
   * @param pValues the value state to use
   * @param pConstraints the constraints state to use
   */
  public ForgettingCompositeState(
      final ValueAnalysisState pValues, final ConstraintsState pConstraints) {
    values = ValueAnalysisState.copyOf(pValues);
    constraints = pConstraints.copyOf();
  }

  public ValueAnalysisState getValueState() {
    return values;
  }

  public ConstraintsState getConstraintsState() {
    return constraints;
  }

  @Override
  public ValueAnalysisInformation forget(final MemoryLocation pLocation) {
    return values.forget(pLocation);
  }

  public void forget(final Constraint pConstraint) {
    assert constraints.contains(pConstraint);
    constraints.remove(pConstraint);
  }

  public void remember(final Constraint pConstraint) {
    constraints.add(pConstraint);
  }

  @Override
  public void remember(
      final MemoryLocation pLocation, final ValueAnalysisInformation pValueInformation) {

    values.remember(pLocation, pValueInformation);
  }

  @Override
  public Set<MemoryLocation> getTrackedMemoryLocations() {
    return values.getTrackedMemoryLocations();
  }

  public Set<Constraint> getTrackedConstraints() {
    return new HashSet<>(constraints);
  }

  /** Returns the size of the wrapped {@link ValueAnalysisState}. */
  @Override
  public int getSize() {
    return values.getSize();
  }

  /** Returns the size of the wrapped {@link ConstraintsState}. */
  public int getConstraintsSize() {
    return constraints.size();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ForgettingCompositeState that = (ForgettingCompositeState) o;

    if (!constraints.equals(that.constraints)) {
      return false;
    }
    if (!values.equals(that.values)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = values.hashCode();
    result = 31 * result + constraints.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ForgettingCompositeState[" + values + ", " + constraints + ']';
  }
}
