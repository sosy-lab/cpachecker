// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class ThreadNode {

  /** The corresponding CFANode of the input CFA. */
  public final CFANode cfaNode;

  /** The corresponding program counter of the CFANode. */
  public final int pc;

  /** The set of context-sensitive return leaving edges of this ThreadNode. */
  public final ImmutableSet<ThreadEdge> leavingEdges;

  public ThreadNode(CFANode pCfaNode, int pPc, ImmutableSet<ThreadEdge> pLeavingEdges) {
    cfaNode = pCfaNode;
    pc = pPc;
    leavingEdges = pLeavingEdges;
  }
}
