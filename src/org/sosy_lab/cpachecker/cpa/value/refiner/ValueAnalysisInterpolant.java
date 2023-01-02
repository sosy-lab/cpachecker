// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class represents a Value-Analysis interpolant, itself, just a mere wrapper around a map from
 * memory locations to values, representing a variable assignment.
 */
public final class ValueAnalysisInterpolant
    implements Interpolant<ValueAnalysisState, ValueAnalysisInterpolant> {

  /** the variable assignment of the interpolant */
  private final @Nullable PersistentMap<MemoryLocation, ValueAndType> assignment;

  /** the interpolant representing "true" */
  public static final ValueAnalysisInterpolant TRUE = new ValueAnalysisInterpolant();

  /** the interpolant representing "false" */
  public static final ValueAnalysisInterpolant FALSE = new ValueAnalysisInterpolant(null);

  /** Constructor for a new, empty interpolant, i.e. the interpolant representing "true" */
  private ValueAnalysisInterpolant() {
    assignment = PathCopyingPersistentTreeMap.of();
  }

  /**
   * Constructor for a new interpolant representing the given variable assignment
   *
   * @param pAssignment the variable assignment to be represented by the interpolant
   */
  public ValueAnalysisInterpolant(PersistentMap<MemoryLocation, ValueAndType> pAssignment) {
    assignment = pAssignment;
  }

  /**
   * This method serves as factory method for an initial, i.e. an interpolant representing "true"
   */
  public static ValueAnalysisInterpolant createInitial() {
    return new ValueAnalysisInterpolant();
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return isFalse() ? ImmutableSet.of() : Collections.unmodifiableSet(assignment.keySet());
  }

  /**
   * This method joins to value-analysis interpolants. If the underlying map contains different
   * values for a key contained in both maps, the behaviour is undefined.
   *
   * @param other the value-analysis interpolant to join with this one
   * @return a new value-analysis interpolant containing the joined mapping of this and the other
   *     value-analysis interpolant
   */
  @Override
  public ValueAnalysisInterpolant join(final ValueAnalysisInterpolant other) {
    if (assignment == null || other.assignment == null) {
      return ValueAnalysisInterpolant.FALSE;
    }

    // add other itp mapping - one by one for now, to check for correctness
    // newAssignment.putAll(other.assignment);
    PersistentMap<MemoryLocation, ValueAndType> newAssignment = assignment;
    for (Entry<MemoryLocation, ValueAndType> entry : other.assignment.entrySet()) {
      if (newAssignment.containsKey(entry.getKey())) {
        assert entry.getValue().equals(other.assignment.get(entry.getKey()))
            : "interpolants mismatch in " + entry.getKey();
      }
      newAssignment = newAssignment.putAndCopy(entry.getKey(), entry.getValue());

      assert Objects.equals(
              entry.getValue().getType(), other.assignment.get(entry.getKey()).getType())
          : "interpolants mismatch in " + entry.getKey();
    }

    return new ValueAnalysisInterpolant(newAssignment);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(assignment);
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
    return Objects.equals(assignment, other.assignment);
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
   * This method serves as factory method to create a value-analysis state from the interpolant
   *
   * @return a value-analysis state that represents the same variable assignment as the interpolant
   */
  @Override
  public ValueAnalysisState reconstructState() {
    if (assignment == null) {
      throw new IllegalStateException("Can't reconstruct state from FALSE-interpolant");
    } else {
      return new ValueAnalysisState(Optional.empty(), assignment);
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

    for (Entry<MemoryLocation, ValueAndType> itp : assignment.entrySet()) {
      if (!valueState.contains(itp.getKey())) {
        valueState.assignConstant(
            itp.getKey(), itp.getValue().getValue(), itp.getValue().getType());
        strengthened = true;

      } else {
        verify(
            valueState.getValueFor(itp.getKey()).asNumericValue().longValue()
                == itp.getValue().getValue().asNumericValue().longValue(),
            "state and interpolant do not match in value for variable %s [state = %s != %s = itp]"
                + " for state %s",
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
   * <p>As the information on what to retain is derived in a static syntactical analysis, the set to
   * retain is a collection of memory location identifiers, instead of {@link MemoryLocation}s, as
   * offsets cannot be provided.
   *
   * @param toRetain the set of memory location identifiers to retain in the interpolant.
   * @return the weakened interpolant
   */
  @SuppressWarnings("ConstantConditions") // isTrivial() checks for FALSE-interpolants
  public ValueAnalysisInterpolant weaken(Set<String> toRetain) {
    if (isTrivial()) {
      return this;
    }

    PersistentMap<MemoryLocation, ValueAndType> weakenedAssignments = assignment;
    for (MemoryLocation current : assignment.keySet()) {
      if (!toRetain.contains(current.getExtendedQualifiedName())) {
        weakenedAssignments = weakenedAssignments.removeAndCopy(current);
      }
    }

    return new ValueAnalysisInterpolant(weakenedAssignments);
  }

  @SuppressWarnings("ConstantConditions") // isTrivial() asserts that assignment != null
  @Override
  public int getSize() {
    return isTrivial() ? 0 : assignment.size();
  }
}
