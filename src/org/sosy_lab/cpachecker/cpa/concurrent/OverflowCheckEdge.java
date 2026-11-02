// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A synthetic no-op edge that ends where {@link #getCheckedEdge()} starts. It is applied right
 * before the checked edge and is not part of the CFA.
 *
 * <p>{@link org.sosy_lab.cpachecker.cpa.overflow.OverflowCPA} checks the edges leaving the
 * successor of each edge for overflows (lookahead). With interleaving, the edge executed after a
 * state may belong to another thread, so the lookahead of a thread's last edge can be stale when
 * the thread moves again. Applying this edge first repeats the lookahead for the checked edge in
 * the state it is executed in.
 */
final class OverflowCheckEdge extends BlankEdge {

  private static final long serialVersionUID = 1L;

  private final CFAEdge checkedEdge;

  OverflowCheckEdge(CFAEdge pCheckedEdge) {
    super(
        "",
        pCheckedEdge.getFileLocation(),
        pCheckedEdge.getPredecessor(),
        pCheckedEdge.getPredecessor(),
        "overflow check");
    checkedEdge = pCheckedEdge;
  }

  CFAEdge getCheckedEdge() {
    return checkedEdge;
  }
}
