// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.collect.MapsDifference.Visitor;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;
import org.sosy_lab.common.collect.PersistentSortedMaps.MergeConflictHandler;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
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

  private final int assignmentsSize;

  private final int numberOfGlobalConstantsInAssignment;

  private final int numberOfSymbolicConstants;

  /** the interpolant representing "true" */
  public static final ValueAnalysisInterpolant TRUE = new ValueAnalysisInterpolant();

  /** the interpolant representing "false" */
  public static final ValueAnalysisInterpolant FALSE = new ValueAnalysisInterpolant(null, 0, 0, 0);

  /** Constructor for a new, empty interpolant, i.e. the interpolant representing "true" */
  private ValueAnalysisInterpolant() {
    assignment = PathCopyingPersistentTreeMap.of();
    assignmentsSize = 0;
    numberOfGlobalConstantsInAssignment = 0;
    numberOfSymbolicConstants = 0;
  }

  /**
   * Constructor for a new interpolant representing the given variable assignment
   *
   * @param pAssignment the variable assignment to be represented by the interpolant
   */
  public ValueAnalysisInterpolant(
      PersistentMap<MemoryLocation, ValueAndType> pAssignment,
      int pAssignmentsSize,
      int pNumberOfGlobalConstants,
      int pNumberOfSymbolicConstants) {
    assignment = pAssignment;
    assignmentsSize = pAssignmentsSize;
    numberOfGlobalConstantsInAssignment = pNumberOfGlobalConstants;
    numberOfSymbolicConstants = pNumberOfSymbolicConstants;
  }

  /**
   * This method serves as factory method for an initial, i.e. an interpolant representing "true"
   */
  public static ValueAnalysisInterpolant createInitial() {
    return new ValueAnalysisInterpolant();
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return isFalse() ? ImmutableSet.of() : assignment.keySet();
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

    int newAssignmentsSize = assignmentsSize;
    int newNumberOfGlobalConstantsInAssignment = numberOfGlobalConstantsInAssignment;
    int newNumberOfSymbolicConstantsInAssignment = numberOfSymbolicConstants;

    // Collects additions of entries from other.assignment not present in assignment
    List<MapsDifference.Entry<MemoryLocation, ValueAndType>> additionsToAssignment =
        new ArrayList<>();

    PersistentMap<MemoryLocation, ValueAndType> newAssignment =
        PersistentSortedMaps.merge(
            (PathCopyingPersistentTreeMap<MemoryLocation, ValueAndType>) assignment,
            (PathCopyingPersistentTreeMap<MemoryLocation, ValueAndType>) other.assignment,
            Equivalence.equals(),
            getLenientMergeConflictHandler(),
            collectRightValueDifferencesOnly(additionsToAssignment));

    for (MapsDifference.Entry<MemoryLocation, ValueAndType> addition : additionsToAssignment) {
      checkArgument(addition.getLeftValue().isEmpty());
      newAssignmentsSize++;
      if (!addition.getKey().isOnFunctionStack()) {
        newNumberOfGlobalConstantsInAssignment++;
      }
      if (addition.getRightValue().orElseThrow().getValue() instanceof SymbolicValue) {
        newNumberOfSymbolicConstantsInAssignment++;
      }
    }

    return new ValueAnalysisInterpolant(
        newAssignment,
        newAssignmentsSize,
        newNumberOfGlobalConstantsInAssignment,
        newNumberOfSymbolicConstantsInAssignment);
  }

  public static MergeConflictHandler<
          org.sosy_lab.cpachecker.util.states.MemoryLocation, ValueAnalysisState.ValueAndType>
      getLenientMergeConflictHandler() {
    return (key, value1, value2) -> {
      assert value1.equals(value2)
          : "interpolants mismatch " + value1 + " and " + value2 + " for " + key;
      return value2;
    };
  }

  // Collects all entries that are not present in the "left" list, but in the "right".
  // Does not collect entries with matching keys, but not matching values.
  // This allows external tracking of details of the resulting map to be calculated by tacking the
  // starting information of the left map and then subtracting the information of the result of this
  // method
  public static <K, V> Visitor<K, V> collectRightValueDifferencesOnly(
      Collection<MapsDifference.Entry<K, V>> collectIn) {
    checkNotNull(collectIn);
    return new Visitor<>() {
      @Override
      public void leftValueOnly(K pKey, V pLeftValue) {}

      @Override
      public void rightValueOnly(K pKey, V pRightValue) {
        collectIn.add(MapsDifference.Entry.forRightValueOnly(pKey, pRightValue));
      }

      @Override
      public void differingValues(K pKey, V pLeftValue, V pRightValue) {
        // Might be useful in the future
        // target.add(MapsDifference.Entry.forDifferingValues(pKey, pLeftValue, pRightValue));
      }
    };
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
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ValueAnalysisInterpolant other = (ValueAnalysisInterpolant) obj;
    return Objects.equals(assignment, other.assignment);
  }

  /**
   * The method checks for trueness of the interpolant.
   *
   * @return whether the interpolant represents "true"
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
      return new ValueAnalysisState(
          Optional.empty(),
          assignment,
          assignmentsSize,
          numberOfGlobalConstantsInAssignment,
          numberOfSymbolicConstants);
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

    // TODO: can we improve the complexity of this?
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
    int newAssignmentsSize = assignmentsSize;
    int newNumberOfGlobalConstantsInAssignment = numberOfGlobalConstantsInAssignment;
    int newNumberOfSymbolicConstantsInAssignment = numberOfSymbolicConstants;
    // TODO: the traversal/removal complexity can most likely be improved
    for (Entry<MemoryLocation, ValueAndType> current : assignment.entrySet()) {
      if (!toRetain.contains(current.getKey().getExtendedQualifiedName())) {
        newAssignmentsSize--;
        if (!current.getKey().isOnFunctionStack()) {
          newNumberOfGlobalConstantsInAssignment--;
        }
        if (current.getValue().getValue() instanceof SymbolicValue) {
          newNumberOfSymbolicConstantsInAssignment--;
        }
        weakenedAssignments = weakenedAssignments.removeAndCopy(current.getKey());
      }
    }

    return new ValueAnalysisInterpolant(
        weakenedAssignments,
        newAssignmentsSize,
        newNumberOfGlobalConstantsInAssignment,
        newNumberOfSymbolicConstantsInAssignment);
  }

  @SuppressWarnings("ConstantConditions") // isTrivial() asserts that assignment != null
  @Override
  public int getSize() {
    return isTrivial() ? 0 : assignment.size();
  }
}
