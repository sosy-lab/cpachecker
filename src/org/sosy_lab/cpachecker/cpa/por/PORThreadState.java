// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public record PORThreadState(
    LocationState pLocationState,
    CallstackState pCallstackState,
    PathFormula pPathFormula,
    List<MemoryEvent> pMemoryEvents) {
  @Override
  public String toString() {
    return "(loc=%s, callstack=%s, pathFormula=%s, memoryEvents=%s)"
        .formatted(pLocationState, pCallstackState, pPathFormula, pMemoryEvents);
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof PORThreadState that)) return false;
    return Objects.equals(pPathFormula, that.pPathFormula)
        && Objects.equals(pLocationState, that.pLocationState)
        && Objects.equals(pCallstackState, that.pCallstackState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pLocationState, pCallstackState, pPathFormula);
  }
}
