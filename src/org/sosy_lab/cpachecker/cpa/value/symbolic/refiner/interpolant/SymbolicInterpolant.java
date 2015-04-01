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

import java.util.Set;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.refiner.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Interpolant for refinement of symbolic value analysis.
 */
public class SymbolicInterpolant implements Interpolant<ForgettingCompositeState> {

  static final SymbolicInterpolant TRUE = new SymbolicInterpolant();
  static final SymbolicInterpolant FALSE = new SymbolicInterpolant(null, null);

  private ValueAnalysisInformation valueInformation;
  private ConstraintsInformation constraintsInformation;


  private SymbolicInterpolant() {
    valueInformation = ValueAnalysisInformation.EMPTY;
    constraintsInformation = ConstraintsInformation.EMPTY;
  }

  protected SymbolicInterpolant(ValueAnalysisInformation pValueInfo,
                                ConstraintsInformation pConstraints) {
    valueInformation = pValueInfo;
    constraintsInformation = pConstraints;
  }

  @Override
  public ForgettingCompositeState reconstructState() {
    final PersistentMap<MemoryLocation, Value> assignments =
        PathCopyingPersistentTreeMap.copyOf(valueInformation.getAssignments());
    final PersistentMap<MemoryLocation, Type> types =
        PathCopyingPersistentTreeMap.copyOf(valueInformation.getLocationTypes());
    final PersistentMap<SymbolicIdentifier, Value> identifierValues =
        PathCopyingPersistentTreeMap.copyOf(valueInformation.getIdentifierValues());

    final ValueAnalysisState values = new ValueAnalysisState(assignments, types, identifierValues);

    ConstraintsState constraints = new ConstraintsState(constraintsInformation.getConstraints(),
                                                        constraintsInformation.getAssignments());

    return new ForgettingCompositeState(values, constraints);
  }

  @Override
  public int getSize() {
    return valueInformation == null ? 0 : valueInformation.getAssignments().size();
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return valueInformation.getAssignments().keySet();
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
  public boolean isTrivial() {
    return isFalse() || isTrue();
  }

  @Override
  public <T extends Interpolant<ForgettingCompositeState>> T join(T otherInterpolant) {
    final ForgettingCompositeState thisState = reconstructState();
    final ForgettingCompositeState otherState = otherInterpolant.reconstructState();

    final ValueAnalysisState thisValues = thisState.getValueState();
    final ValueAnalysisState otherValues = otherState.getValueState();

    ValueAnalysisState newValues = thisValues.join(otherValues);

    final ConstraintsState thisConstraints = thisState.getConstraintsState();
    final ConstraintsState otherConstraints = otherState.getConstraintsState();

    ConstraintsState newConstraints;

    if (thisConstraints.size() > otherConstraints.size()) {
      newConstraints = thisConstraints.copyOf();
      newConstraints.retainAll(otherConstraints);
    } else {
      newConstraints = otherConstraints.copyOf();
      newConstraints.retainAll(thisConstraints);
    }

    return (T) new SymbolicInterpolant(newValues.getInformation(),
                                       new ConstraintsInformation(newConstraints,
                                                                  new IdentifierAssignment()));
  }

  @Override
  public String toString() {
    return "Interpolant[" + valueInformation
        + ", " + constraintsInformation + "]";
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

    if (constraintsInformation == null) {
      return that.constraintsInformation == null;
    }

    if (valueInformation == null) {
      return that.valueInformation == null;
    }

    return constraintsInformation.equals(that.constraintsInformation)
        && valueInformation.equals(that.valueInformation);
  }

  @Override
  public int hashCode() {
    int result = valueInformation != null ? valueInformation.hashCode() : 0;
    result = 31 * result +
        (constraintsInformation != null ? constraintsInformation.hashCode() : 0);
    return result;
  }
}
