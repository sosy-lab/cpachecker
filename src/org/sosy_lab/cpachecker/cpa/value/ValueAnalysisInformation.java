// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Information about value assignments needed for symbolic interpolation. */
public final class ValueAnalysisInformation {

  public static final ValueAnalysisInformation EMPTY = new ValueAnalysisInformation();

  private final PersistentMap<MemoryLocation, ValueAndType> assignments;

  private final int assignmentsSize;

  private final int numberOfGlobalConstantsInAssignment;

  private final int numberOfSymbolicConstantsInAssignment;

  ValueAnalysisInformation(
      final PersistentMap<MemoryLocation, ValueAndType> pAssignments,
      int pAssignmentsSize,
      int pNumberOfGlobalConstants,
      int pNumberOfSymbolicConstants) {
    assignments = pAssignments;
    assignmentsSize = pAssignmentsSize;
    numberOfGlobalConstantsInAssignment = pNumberOfGlobalConstants;
    numberOfSymbolicConstantsInAssignment = pNumberOfSymbolicConstants;
  }

  private ValueAnalysisInformation() {
    assignments = PathCopyingPersistentTreeMap.of();
    assignmentsSize = 0;
    numberOfGlobalConstantsInAssignment = 0;
    numberOfSymbolicConstantsInAssignment = 0;
  }

  public PersistentMap<MemoryLocation, ValueAndType> getAssignments() {
    return assignments;
  }

  public int getAssignmentsSize() {
    return assignmentsSize;
  }

  public int getNumberOfGlobalConstantsInAssignment() {
    return numberOfGlobalConstantsInAssignment;
  }

  public int getNumberOfSymbolicConstantsInAssignment() {
    return numberOfSymbolicConstantsInAssignment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ValueAnalysisInformation that = (ValueAnalysisInformation) o;
    return assignments.equals(that.assignments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignments);
  }

  @Override
  public String toString() {
    return "ValueInformation[" + assignments + "]";
  }
}
