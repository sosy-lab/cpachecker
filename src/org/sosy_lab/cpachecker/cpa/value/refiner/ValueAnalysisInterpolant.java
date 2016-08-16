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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import static com.google.common.base.Verify.verify;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * This class represents a Value-Analysis interpolant, itself, just a mere wrapper around a map
 * from memory locations to values, representing a variable assignment.
 */
public class ValueAnalysisInterpolant implements Interpolant<ValueAnalysisState> {
  /**
   * the variable assignment of the interpolant
   */
  private @Nullable final Map<MemoryLocation, Value> assignment;
  private @Nullable final Map<MemoryLocation, Type> assignmentTypes;

  /**
   * the interpolant representing "true"
   */
  public static final ValueAnalysisInterpolant TRUE  = new ValueAnalysisInterpolant();

  /**
   * the interpolant representing "false"
   */
  public static final ValueAnalysisInterpolant FALSE = new ValueAnalysisInterpolant((Map<MemoryLocation, Value>)null,(Map<MemoryLocation, Type>)null);

  /**
   * Constructor for a new, empty interpolant, i.e. the interpolant representing "true"
   */
  private ValueAnalysisInterpolant() {
    assignment = new HashMap<>();
    assignmentTypes = new HashMap<>();
  }

  /**
   * Constructor for a new interpolant representing the given variable assignment
   *
   * @param pAssignment the variable assignment to be represented by the interpolant
   */
  public ValueAnalysisInterpolant(Map<MemoryLocation, Value> pAssignment, Map<MemoryLocation, Type> pAssignmentToType) {
    assignment = pAssignment;
    assignmentTypes = pAssignmentToType;
  }

  /**
   * This method serves as factory method for an initial, i.e. an interpolant representing "true"
   */
  public static ValueAnalysisInterpolant createInitial() {
    return new ValueAnalysisInterpolant();
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return isFalse()
        ? Collections.<MemoryLocation>emptySet()
        : Collections.unmodifiableSet(assignment.keySet());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Interpolant<ValueAnalysisState>> T join(final T pOther) {
    assert pOther instanceof ValueAnalysisInterpolant;

    return (T) join0((ValueAnalysisInterpolant) pOther);
  }

  /**
   * This method joins to value-analysis interpolants. If the underlying map contains different values for a key
   * contained in both maps, the behaviour is undefined.
   *
   * @param other the value-analysis interpolant to join with this one
   * @return a new value-analysis interpolant containing the joined mapping of this and the other value-analysis
   * interpolant
   */
  private ValueAnalysisInterpolant join0(ValueAnalysisInterpolant other) {

    if (assignment == null || other.assignment == null) {
      return ValueAnalysisInterpolant.FALSE;
    }

    Map<MemoryLocation, Value> newAssignment = new HashMap<>(assignment);

    // add other itp mapping - one by one for now, to check for correctness
    // newAssignment.putAll(other.assignment);
    for (Map.Entry<MemoryLocation, Value> entry : other.assignment.entrySet()) {
      if (newAssignment.containsKey(entry.getKey())) {
        assert (entry.getValue().equals(other.assignment.get(entry.getKey()))) : "interpolants mismatch in " + entry.getKey();
      }

      newAssignment.put(entry.getKey(), entry.getValue());
    }

    Map<MemoryLocation, Type> newAssignmentTypes = new HashMap<>();
    for (MemoryLocation loc : newAssignment.keySet()) {
      if (assignmentTypes.containsKey(loc)) {
        newAssignmentTypes.put(loc, assignmentTypes.get(loc));
      } else {
        newAssignmentTypes.put(loc, other.assignmentTypes.get(loc));
      }
    }


    return new ValueAnalysisInterpolant(newAssignment, newAssignmentTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(assignment) + Objects.hashCode(assignmentTypes);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    ValueAnalysisInterpolant other = (ValueAnalysisInterpolant) obj;
    return Objects.equals(assignment, other.assignment) && Objects.equals(
        assignmentTypes, other.assignmentTypes);
  }

  /**
   * The method checks for trueness of the interpolant.
   *
   * @return true, if the interpolant represents "true", else false
   */
  @Override
  public boolean isTrue() {
    return !isFalse() && assignment.isEmpty();
  }

  /**
   * The method checks for falseness of the interpolant.
   *
   * @return true, if the interpolant represents "false", else true
   */
  @Override
  public boolean isFalse() {
    return assignment == null;
  }

  /**
   * The method checks if the interpolant is a trivial one, i.e. if it represents either true or false
   *
   * @return true, if the interpolant is trivial, else false
   */
  @Override
  public boolean isTrivial() {
    return isFalse() || isTrue();
  }

  /**
   * This method serves as factory method to create a value-analysis state from the interpolant
   *
   * @return a value-analysis state that represents the same variable assignment as the interpolant
   */
  @Override
  public ValueAnalysisState reconstructState() {
    if (assignment == null || assignmentTypes == null) {
      throw new IllegalStateException("Can't reconstruct state from FALSE-interpolant");

    } else {
      return new ValueAnalysisState(
          Optional.empty(),
          PathCopyingPersistentTreeMap.copyOf(assignment),
          PathCopyingPersistentTreeMap.copyOf(assignmentTypes));
    }
  }

  @Override
  public String toString() {
    if (isFalse()) {
      return "FALSE";
    }

    if (isTrue()) {
      return "TRUE";
    }

    return assignment.toString();
  }

  public boolean strengthen(ValueAnalysisState valueState, ARGState argState) {
    if (isTrivial()) {
      return false;
    }

    boolean strengthened = false;

    for (Map.Entry<MemoryLocation, Value> itp : assignment.entrySet()) {
      if (!valueState.contains(itp.getKey())) {
        valueState.assignConstant(itp.getKey(), itp.getValue(), assignmentTypes.get(itp.getKey()));
        strengthened = true;

      } else {
        verify(
            valueState.getValueFor(itp.getKey()).asNumericValue().longValue()
                == itp.getValue().asNumericValue().longValue(),
            "state and interpolant do not match in value for variable %s [state = %s != %s = itp] for state %s",
            itp.getKey(),
            valueState.getValueFor(itp.getKey()),
            itp.getValue(),
            argState.getStateId());
      }
    }

    return strengthened;
  }

  /**
   * This method weakens the interpolant to the given set of memory location identifiers.
   *
   * As the information on what to retain is derived in a static syntactical analysis, the set to retain is a
   * collection of memory location identifiers, instead of {@link MemoryLocation}s, as offsets cannot be provided.
   *
   * @param toRetain the set of memory location identifiers to retain in the interpolant.
   * @return the weakened interpolant
   */
  @SuppressWarnings("ConstantConditions") // isTrivial() checks for FALSE-interpolants
  public ValueAnalysisInterpolant weaken(Set<String> toRetain) {
    if (isTrivial()) {
      return this;
    }

    ValueAnalysisInterpolant weakenedItp = new ValueAnalysisInterpolant(new HashMap<>(assignment), new HashMap<>(assignmentTypes));

    for (Iterator<MemoryLocation> it = weakenedItp.assignment.keySet().iterator(); it.hasNext(); ) {
      MemoryLocation current = it.next();

      if (!toRetain.contains(current.getAsSimpleString())) {
        it.remove();
      }
    }

    return weakenedItp;
  }

  @SuppressWarnings("ConstantConditions") // isTrivial() asserts that assignment != null
  @Override
  public int getSize() {
    return isTrivial() ? 0 : assignment.size();
  }
}