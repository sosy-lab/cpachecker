// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

public record PORThreadState(LocationState pLocationState, CallstackState pCallstackState) {
  @Override
  public String toString() {
    return "(loc=%s, callstack=%s)".formatted(pLocationState, pCallstackState);
  }
}
