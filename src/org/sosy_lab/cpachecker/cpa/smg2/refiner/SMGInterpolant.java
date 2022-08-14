// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class represents a SMG-Value-Analysis interpolant, itself, just a mere wrapper around a map from
 * memory locations to values, representing a variable assignment.
 */
public final class SMGInterpolant
    implements Interpolant<SMGState, SMGInterpolant> {

  /** State information. Null for true and false! * */
  private final @Nullable SMGState originalState;

  /** the variable assignment of the interpolant */
  private final @Nullable PersistentMap<MemoryLocation, ValueAndValueSize> nonHeapAssignments;

  private final @Nullable Map<String, BigInteger> variableNameToMemorySizeInBits;

  private final @Nullable Map<String, CType> variableToTypeMap;

  /** Constructor for a new, empty interpolant, i.e. the interpolant representing "true" */
  private SMGInterpolant() {
    this.originalState = null;
    nonHeapAssignments = PathCopyingPersistentTreeMap.of();
    variableNameToMemorySizeInBits = new HashMap<>();
    variableToTypeMap = new HashMap<>();
  }

  /**
   * Constructor for a new interpolant representing the given variable assignment
   *
   * @param pNonHeapAssignments the variable assignment to be represented by the interpolant
   */
  public SMGInterpolant(
      PersistentMap<MemoryLocation, ValueAndValueSize> pNonHeapAssignments,
      Map<String, BigInteger> pVariableNameToMemorySizeInBits,
      Map<String, CType> pVariableToTypeMap,
      SMGState pOriginalState) {
    originalState = pOriginalState;
    nonHeapAssignments = pNonHeapAssignments;
    variableNameToMemorySizeInBits = pVariableNameToMemorySizeInBits;
    variableToTypeMap = pVariableToTypeMap;
  }

  /**
   * This method serves as factory method for an initial, i.e. an interpolant representing "true"
   */
  public static SMGInterpolant createInitial() {
    return new SMGInterpolant();
  }

  /** the interpolant representing "true" */
  public static SMGInterpolant createTRUE() {
    return createInitial();
  }

  /** the interpolant representing "false" */
  public static SMGInterpolant createFALSE() {
    return new SMGInterpolant(null, null, null, null);
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return isFalse() ? ImmutableSet.of() : Collections.unmodifiableSet(nonHeapAssignments.keySet());
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
  public SMGInterpolant join(final SMGInterpolant other) {
    // We expect that if nonHeapAssignments != null all other nullables are not null also except for
    // maybe the state!
    if (nonHeapAssignments == null || other.nonHeapAssignments == null) {
      return createFALSE();
    }

    // add other itp mapping - one by one for now, to check for correctness
    // newAssignment.putAll(other.assignment);
    PersistentMap<MemoryLocation, ValueAndValueSize> newAssignment = nonHeapAssignments;
    for (Entry<MemoryLocation, ValueAndValueSize> entry : other.nonHeapAssignments.entrySet()) {
      if (newAssignment.containsKey(entry.getKey())) {
        assert entry.getValue().equals(other.nonHeapAssignments.get(entry.getKey()))
            : "interpolants mismatch in " + entry.getKey();
      }
      newAssignment = newAssignment.putAndCopy(entry.getKey(), entry.getValue());
      String thisEntryQualName = entry.getKey().getQualifiedName();
      variableNameToMemorySizeInBits.put(
          thisEntryQualName, other.variableNameToMemorySizeInBits.get(thisEntryQualName));
      variableToTypeMap.put(thisEntryQualName, other.variableToTypeMap.get(thisEntryQualName));

      assert Objects.equals(
              entry.getValue().getSizeInBits(),
              other.nonHeapAssignments.get(entry.getKey()).getSizeInBits())
          : "interpolants mismatch in " + entry.getKey();
    }
    if (originalState != null) {
    return new SMGInterpolant(
        newAssignment, variableNameToMemorySizeInBits, variableToTypeMap, originalState);
    } else {
      return new SMGInterpolant(
          newAssignment, variableNameToMemorySizeInBits, variableToTypeMap, other.originalState);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(nonHeapAssignments);
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

    SMGInterpolant other = (SMGInterpolant) obj;
    // technically this is not correct as we leave out the heap. But thats ok for now.
    return Objects.equals(nonHeapAssignments, other.nonHeapAssignments);
  }

  /**
   * The method checks for trueness of the interpolant.
   *
   * @return true, if the interpolant represents "true", else false
   */
  @Override
  public boolean isTrue() {
    return !isFalse() && nonHeapAssignments.isEmpty();
  }

  /**
   * The method checks for falseness of the interpolant.
   *
   * @return true, if the interpolant represents "false", else true
   */
  @Override
  public boolean isFalse() {
    return nonHeapAssignments == null;
  }

  /**
   * This method serves as factory method to create a smg2 state from the interpolant
   *
   * @return a smg2 state that represents the same variable assignment as the interpolant
   */
  @Override
  public SMGState reconstructState() {
    if (nonHeapAssignments == null) {
      throw new IllegalStateException("Can't reconstruct state from FALSE-interpolant");
    } else {
      try {
        return originalState.reconstructSMGStateFromNonHeapAssignments(
            nonHeapAssignments, variableNameToMemorySizeInBits, variableToTypeMap);
      } catch (SMG2Exception e) {
        // Should actually never happen. This exception gets thrown for over/underwrites
        // But since we copy legit values 1:1 this does not happen.
        return originalState;
      }
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

    return nonHeapAssignments.toString();
  }

  public boolean strengthen(SMGState state, ARGState argState) {
    if (isTrivial()) {
      return false;
    }

    boolean strengthened = false;
    SMGState currentState = state;

    for (Entry<MemoryLocation, ValueAndValueSize> itp : nonHeapAssignments.entrySet()) {
      if (!currentState.isLocalOrGlobalVariablePresent(itp.getKey())) {
        try {
          currentState =
              currentState.assignNonHeapConstant(
                  itp.getKey(), itp.getValue(), variableNameToMemorySizeInBits, variableToTypeMap);
        } catch (SMG2Exception e) {
          // Critical error
          throw new RuntimeException(e);
        }
        strengthened = true;

      } else {
        verify(
            currentState.verifyVariableEqualityWithValueAt(itp.getKey(), itp.getValue()),
            "state and interpolant do not match in value for variable %s [state = %s != %s = itp]"
                + " for state %s",
            itp.getKey(),
            currentState.getValueToVerify(itp.getKey(), itp.getValue()),
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
  public SMGInterpolant weaken(Set<String> toRetain) {
    if (isTrivial()) {
      return this;
    }

    PersistentMap<MemoryLocation, ValueAndValueSize> weakenedAssignments = nonHeapAssignments;
    for (MemoryLocation current : nonHeapAssignments.keySet()) {
      if (!toRetain.contains(current.getExtendedQualifiedName())) {
        weakenedAssignments = weakenedAssignments.removeAndCopy(current);
        variableNameToMemorySizeInBits.remove(current.getQualifiedName());
        // We don't delete types out of variableToTypeMap because in for example arrays we have
        // multiple entries, one for each offset. We don't want to delete the type because of that.
        // If it were to change it would be overridden.
      }
    }

    return new SMGInterpolant(
        weakenedAssignments, variableNameToMemorySizeInBits, variableToTypeMap, originalState);
  }

  @SuppressWarnings("ConstantConditions") // isTrivial() asserts that assignment != null
  @Override
  public int getSize() {
    return isTrivial() ? 0 : nonHeapAssignments.size();
  }
}
