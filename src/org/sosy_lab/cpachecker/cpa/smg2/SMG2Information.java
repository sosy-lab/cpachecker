// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Information about value assignments needed for symbolic interpolation. */
public final class SMG2Information {

  // SMGs need to keep track of the state
  private final SMGState state;
  private final PersistentMap<MemoryLocation, ValueAndValueSize> nonHeapAssignments;

  SMG2Information(
      final PersistentMap<MemoryLocation, ValueAndValueSize> pAssignments, SMGState pState) {
    nonHeapAssignments = pAssignments;
    state = pState;
  }

  private SMG2Information(SMGState pState) {
    nonHeapAssignments = PathCopyingPersistentTreeMap.of();
    state = pState;
  }

  public static SMG2Information getEmptySMG2Information(SMGState pState) {
    return new SMG2Information(pState);
  }

  public PersistentMap<MemoryLocation, ValueAndValueSize> getAssignments() {
    return nonHeapAssignments;
  }

  public SMGState getSMGState() {
    return state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SMG2Information that = (SMG2Information) o;
    return nonHeapAssignments.equals(that.nonHeapAssignments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nonHeapAssignments) + 31 * state.hashCode();
  }

  @Override
  public String toString() {
    return "ValueInformation[" + nonHeapAssignments + "]";
  }
}
