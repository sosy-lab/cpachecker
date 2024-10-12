// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;

public class ThreadNode {

  /** The corresponding CFANode of the input CFA. */
  public final CFANode cfaNode;

  /** The corresponding program counter of the CFANode. */
  public final int pc;

  /** The list of context-sensitive return leaving edges of this ThreadNode. */
  private final List<ThreadEdge> leavingEdges;

  protected ThreadNode(CFANode pCfaNode, int pPc, List<ThreadEdge> pLeavingEdges) {
    cfaNode = pCfaNode;
    pc = pPc;
    leavingEdges = pLeavingEdges;
  }

  public List<ThreadEdge> leavingEdges() {
    return leavingEdges;
  }

  protected void pruneLeavingEdge(ThreadEdge pThreadEdge) {
    checkArgument(leavingEdges.contains(pThreadEdge), "pThreadEdge not in threadEdges");
    checkArgument(
        pThreadEdge.cfaEdge instanceof CFunctionReturnEdge,
        "only CFunctionReturnEdges can be pruned");

    leavingEdges.remove(pThreadEdge);
  }
}
