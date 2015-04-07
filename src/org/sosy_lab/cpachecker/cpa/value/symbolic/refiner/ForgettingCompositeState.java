/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import java.util.Set;

import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.constraints.util.ConstraintsInformation;
import org.sosy_lab.cpachecker.util.refiner.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * A composite state of {@link ValueAnalysisState} and {@link ConstraintsState}
 * that allows to remove and re-add values.
 */
public class ForgettingCompositeState
    implements ForgetfulState<ForgettingCompositeState.MemoryLocationAssociation> {

  private final ValueAnalysisState values;
  private final ConstraintsState constraints;

  public static ForgettingCompositeState getInitialState() {
    return new ForgettingCompositeState(new ValueAnalysisState(),
                                        new ConstraintsState());
  }

  /**
   * Creates a new state with the given value analysis state and constraints state.
   *
   * @param pValues the value state to use
   * @param pConstraints the constraints state to use
   */
  public ForgettingCompositeState(
      final ValueAnalysisState pValues,
      final ConstraintsState pConstraints
  ) {
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
  public MemoryLocationAssociation forget(final MemoryLocation pLocation) {
    final ValueAnalysisInformation forgottenInformation = values.forget(pLocation);

    ConstraintsInformation forgottenConstraints;

    /*if (forgottenValue instanceof SymbolicExpression) {
      forgottenConstraints = constraints.forget((SymbolicExpression) forgottenValue);
    } else {*/

    forgottenConstraints = ConstraintsInformation.EMPTY;
    //}

    return new MemoryLocationAssociation(pLocation,
                                         forgottenInformation,
                                         forgottenConstraints);
  }

  @Override
  public void remember(
      final MemoryLocation pLocation,
      final MemoryLocationAssociation pValueAndConstraints
  ) {
    final ValueAnalysisInformation valueInfoToRemember = pValueAndConstraints.getValue();

    values.remember(pLocation, valueInfoToRemember);
    // don't remember anything as long as we don't remove anything
    // final ConstraintsInformation constraintsToAdd = pValueAndConstraints.getConstraints();
    // constraints.remember(pLocation, constraintsToAdd);
  }

  @Override
  public Set<MemoryLocation> getTrackedMemoryLocations() {
    return values.getConstantsMapView().keySet();
  }

  /**
   * Returns the size of the wrapped {@link ValueAnalysisState}.
   */
  @Override
  public int getSize() {
    return values.getSize();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ForgettingCompositeState that = (ForgettingCompositeState)o;

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
    return "ForgettingCompositeState[" +
        values +
        ", " + constraints +
        ']';
  }

  /**
   * This class contains a {@link MemoryLocation} and all values of a ValueAnalysisState
   * and all constraints of a ConstraintsState that are associated with it.
   */
  public static class MemoryLocationAssociation {
    private final MemoryLocation location;
    private final ValueAnalysisInformation values;

    private final ConstraintsInformation constraints;

    /**
     * Creates a new <code>MemoryLocationAssociation</code> of the given parameters.
     */
    public MemoryLocationAssociation(
        final MemoryLocation pLocation,
        final ValueAnalysisInformation pValues,
        final ConstraintsInformation pConstraints
    ) {
      location = pLocation;
      values = pValues;
      constraints = pConstraints;
    }

    public MemoryLocation getLocation() {
      return location;
    }

    public ValueAnalysisInformation getValue() {
     return values;
    }

    public ConstraintsInformation getConstraints() {
      return constraints;
    }
  }
}
