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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.util.ConstraintsInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Interpolant for refinement of symbolic value analysis. */
public class SymbolicInterpolant
    implements Interpolant<ForgettingCompositeState, SymbolicInterpolant> {

  static final SymbolicInterpolant TRUE = new SymbolicInterpolant();
  static final SymbolicInterpolant FALSE =
      new SymbolicInterpolant((ValueAnalysisInterpolant) null, null);

  private final ValueAnalysisInterpolant valueInterpolant;
  private final ConstraintsInformation constraintsInformation;

  private SymbolicInterpolant() {
    valueInterpolant = ValueAnalysisInterpolant.createInitial();
    constraintsInformation = ConstraintsInformation.EMPTY;
  }

  public SymbolicInterpolant(
      final ValueAnalysisInformation pValueInfo, final ConstraintsInformation pConstraints) {
    checkNotNull(pValueInfo);
    checkNotNull(pConstraints);
    valueInterpolant = new ValueAnalysisInterpolant(pValueInfo.getAssignments());
    constraintsInformation = pConstraints;
  }

  private SymbolicInterpolant(
      final ValueAnalysisInterpolant pValueInterpolant, final ConstraintsInformation pConstraints) {
    valueInterpolant = pValueInterpolant;
    constraintsInformation = pConstraints;
  }

  @Override
  public ForgettingCompositeState reconstructState() {
    final ValueAnalysisState values = valueInterpolant.reconstructState();
    ConstraintsState constraints = new ConstraintsState(constraintsInformation.getConstraints());
    return new ForgettingCompositeState(values, constraints);
  }

  @Override
  public int getSize() {
    return valueInterpolant == null ? 0 : valueInterpolant.getSize();
  }

  public int getConstraintsSize() {
    return constraintsInformation.getConstraints().size();
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return valueInterpolant.getMemoryLocations();
  }

  public Set<Constraint> getConstraints() {
    return constraintsInformation.getConstraints();
  }

  @Override
  public boolean isTrue() {
    return equals(TRUE);
  }

  @Override
  public boolean isFalse() {
    return equals(FALSE);
  }

  @Override
  public SymbolicInterpolant join(SymbolicInterpolant pOther) {
    ValueAnalysisInterpolant newValueInterpolant = valueInterpolant.join(pOther.valueInterpolant);
    ConstraintsInformation newConstraintsInfo = joinConstraints(pOther.constraintsInformation);
    return new SymbolicInterpolant(newValueInterpolant, newConstraintsInfo);
  }

  /**
   * Join the given {@link ConstraintsInformation} with this object's <code>ConstraintsInformation
   * </code>. The conjunction of both constraints informations' constraints in combination with
   * their definite assignments must not be contradicting. Otherwise, behavior is undefined.
   *
   * @param pOtherConstraintsInfo the constraints information to join with this object's one
   * @return the join of both constraints informations
   */
  private ConstraintsInformation joinConstraints(
      final ConstraintsInformation pOtherConstraintsInfo) {
    // Just use all the constraints of both interpolants.
    // We can't check at this point whether they are contradicting without creating a solver
    // environment, unfortunately.
    Set<Constraint> allConstraints = new HashSet<>(constraintsInformation.getConstraints());
    allConstraints.addAll(pOtherConstraintsInfo.getConstraints());
    return new ConstraintsInformation(allConstraints);
  }

  @Override
  public String toString() {
    return "Interpolant[" + valueInterpolant + ", " + constraintsInformation + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SymbolicInterpolant that = (SymbolicInterpolant)o;
    return Objects.equals(constraintsInformation, that.constraintsInformation)
        && Objects.equals(valueInterpolant, that.valueInterpolant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valueInterpolant, constraintsInformation);
  }
}
