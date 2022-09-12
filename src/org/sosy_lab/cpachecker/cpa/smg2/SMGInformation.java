// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;
import org.sosy_lab.cpachecker.cpa.smg2.util.CFunctionDeclarationAndOptionalValue;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Information about value assignments needed for symbolic interpolation. */
public final class SMGInformation {

  // Mappings needed to restore heap values
  private final Map<SMGObject, Set<SMGHasValueEdge>> heapValuesRemoved;

  // Mapping needed to restore program variables
  private final PersistentMap<String, CType> variableToTypeMap;
  private final Map<String, BigInteger> variableNameAndSizeInBits;
  private final PersistentMap<MemoryLocation, ValueAndValueSize> nonHeapAssignments;
  private final PersistentStack<CFunctionDeclarationAndOptionalValue> stackDeclarations;

  SMGInformation(
      final PersistentMap<MemoryLocation, ValueAndValueSize> pAssignments,
      final Map<String, BigInteger> pVariableNameAndSizeInBits,
      final PersistentMap<String, CType> pVariableToTypeMap,
      final PersistentStack<CFunctionDeclarationAndOptionalValue> pStackDeclarations) {
    nonHeapAssignments = pAssignments;
    variableNameAndSizeInBits = pVariableNameAndSizeInBits;
    variableToTypeMap = pVariableToTypeMap;
    stackDeclarations = pStackDeclarations;
    heapValuesRemoved = ImmutableMap.of();
  }

  SMGInformation(Map<SMGObject, Set<SMGHasValueEdge>> pHeapValuesRemoved) {
    nonHeapAssignments = PathCopyingPersistentTreeMap.of();
    variableNameAndSizeInBits = new HashMap<>();
    variableToTypeMap = PathCopyingPersistentTreeMap.of();
    stackDeclarations = PersistentStack.of();
    heapValuesRemoved = pHeapValuesRemoved;
  }

  private SMGInformation() {
    nonHeapAssignments = PathCopyingPersistentTreeMap.of();
    variableNameAndSizeInBits = new HashMap<>();
    variableToTypeMap = PathCopyingPersistentTreeMap.of();
    stackDeclarations = PersistentStack.of();
    heapValuesRemoved = ImmutableMap.of();
  }

  public static SMGInformation getEmptySMGInformation() {
    return new SMGInformation();
  }

  public Map<SMGObject, Set<SMGHasValueEdge>> getHeapValuesPerObjectMap() {
    return heapValuesRemoved;
  }

  public PersistentMap<MemoryLocation, ValueAndValueSize> getAssignments() {
    return nonHeapAssignments;
  }

  /**
   * @return a map from qualified variable names to their types.
   */
  public PersistentMap<String, CType> getTypeOfVariablesMap() {
    return variableToTypeMap;
  }

  /**
   * @return map from qualified variable name to their sizes in bits.
   */
  public Map<String, BigInteger> getSizeInformationForVariablesMap() {
    return variableNameAndSizeInBits;
  }

  /* Reversed stack of declarations for the stack frames used. i.e. main is on the very top. */
  public PersistentStack<CFunctionDeclarationAndOptionalValue>
      getDeclarationsForStackframesReversed() {
    return stackDeclarations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SMGInformation that = (SMGInformation) o;
    // Under the general assumption that variable names are unique (they aren't really) this should
    // suffice as long as heap is not tracked
    return nonHeapAssignments.equals(that.nonHeapAssignments)
        && heapValuesRemoved.equals(that.heapValuesRemoved);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nonHeapAssignments)
        + 31 * Objects.hash(variableToTypeMap)
        + 17 * Objects.hash(variableNameAndSizeInBits)
        + 23 * Objects.hash(heapValuesRemoved);
  }

  @Override
  public String toString() {
    return "VariableInformation["
        + nonHeapAssignments
        + "]"
        + "\n"
        + "Heap Values ["
        + heapValuesRemoved
        + "]";
  }
}
