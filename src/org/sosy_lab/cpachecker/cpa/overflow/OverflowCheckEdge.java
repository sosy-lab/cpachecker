// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.overflow;

import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * A synthetic no-op edge that makes {@link OverflowTransferRelation} check whether {@link
 * #getCheckedEdge()} overflows in the current state. It is applied right before the checked edge
 * and is not part of the CFA.
 *
 * <p>Needed when {@link OverflowCPA}'s lookahead is disabled, i.e., when the edge executed after a
 * state is not necessarily one leaving the successor of the edge that produced it.
 */
public final class OverflowCheckEdge extends BlankEdge {

  private static final long serialVersionUID = 1L;

  private final CFAEdge checkedEdge;

  public OverflowCheckEdge(CFAEdge pCheckedEdge) {
    super(
        "",
        pCheckedEdge.getFileLocation(),
        pCheckedEdge.getPredecessor(),
        // A fresh node, so that no analysis treats the check as reaching a loop head or exit.
        new CFANode(pCheckedEdge.getPredecessor().getFunction()),
        "overflow check");
    checkedEdge = pCheckedEdge;
  }

  public CFAEdge getCheckedEdge() {
    return checkedEdge;
  }
}
