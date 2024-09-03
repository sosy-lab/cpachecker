// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class ThreadNode {

  /** The corresponding CFANode of the input CFA. */
  public final CFANode node;

  /** The corresponding program counter of the CFANode. */
  public final int pc;

  /** The set of context-sensitive return leaving edges of {@link ThreadNode#node}. */
  public final ImmutableSet<CFAEdge> leavingEdges;

  public ThreadNode(CFANode pNode, int pPc, ImmutableSet<CFAEdge> pLeavingEdges) {
    node = pNode;
    pc = pPc;
    leavingEdges = pLeavingEdges;
  }
}
