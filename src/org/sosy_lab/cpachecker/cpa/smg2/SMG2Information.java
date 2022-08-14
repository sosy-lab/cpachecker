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

  public static final SMG2Information EMPTY = new SMG2Information();

  private final PersistentMap<MemoryLocation, ValueAndValueSize> nonHeapAssignments;

  SMG2Information(final PersistentMap<MemoryLocation, ValueAndValueSize> pAssignments) {
    nonHeapAssignments = pAssignments;
  }

  private SMG2Information() {
    nonHeapAssignments = PathCopyingPersistentTreeMap.of();
  }

  public PersistentMap<MemoryLocation, ValueAndValueSize> getAssignments() {
    return nonHeapAssignments;
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
    return Objects.hash(nonHeapAssignments);
  }

  @Override
  public String toString() {
    return "ValueInformation[" + nonHeapAssignments + "]";
  }
}
