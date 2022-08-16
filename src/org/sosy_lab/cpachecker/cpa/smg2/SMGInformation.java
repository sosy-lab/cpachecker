// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Information about value assignments needed for symbolic interpolation. */
public final class SMGInformation {

  private final PersistentMap<String, CType> variableToTypeMap;
  private final Map<String, BigInteger> variableNameAndSizeInBits;
  private final PersistentMap<MemoryLocation, ValueAndValueSize> nonHeapAssignments;

  SMGInformation(
      final PersistentMap<MemoryLocation, ValueAndValueSize> pAssignments,
      final Map<String, BigInteger> pVariableNameAndSizeInBits,
      final PersistentMap<String, CType> pVariableToTypeMap) {
    nonHeapAssignments = pAssignments;
    variableNameAndSizeInBits = pVariableNameAndSizeInBits;
    variableToTypeMap = pVariableToTypeMap;
  }

  private SMGInformation() {
    nonHeapAssignments = PathCopyingPersistentTreeMap.of();
    variableNameAndSizeInBits = new HashMap<>();
    variableToTypeMap = PathCopyingPersistentTreeMap.of();
  }

  public static SMGInformation getEmptySMGInformation() {
    return new SMGInformation();
  }

  public PersistentMap<MemoryLocation, ValueAndValueSize> getAssignments() {
    return nonHeapAssignments;
  }

  /** @return a map from qualified variable names to their types. */
  public PersistentMap<String, CType> getTypeOfVariablesMap() {
    return variableToTypeMap;
  }

  /** @return map from qualified variable name to their sizes in bits. */
  public Map<String, BigInteger> getSizeInformationForVariablesMap() {
    return variableNameAndSizeInBits;
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
    return nonHeapAssignments.equals(that.nonHeapAssignments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nonHeapAssignments)
        + 31 * Objects.hash(variableToTypeMap)
        + 17 * Objects.hash(variableNameAndSizeInBits);
  }

  @Override
  public String toString() {
    return "ValueInformation[" + nonHeapAssignments + "]";
  }
}