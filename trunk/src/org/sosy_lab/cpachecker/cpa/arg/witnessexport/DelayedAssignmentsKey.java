// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

class DelayedAssignmentsKey {

  private final String from;

  private final CFAEdge edge;

  private final ARGState state;

  public DelayedAssignmentsKey(String pFrom, CFAEdge pEdge, ARGState pState) {
    from = pFrom;
    edge = pEdge;
    state = pState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, edge, state);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof DelayedAssignmentsKey) {
      DelayedAssignmentsKey other = (DelayedAssignmentsKey) pObj;
      return Objects.equals(from, other.from)
          && Objects.equals(edge, other.edge)
          && Objects.equals(state, other.state);
    }
    return false;
  }
}
