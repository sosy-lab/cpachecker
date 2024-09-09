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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class ThreadCFA {
  /** FunctionEntryNode of the main function (main thread) or start routine (pthreads). */
  public final FunctionEntryNode entryNode;

  /**
   * FunctionExitNode of the main function (main thread) or start routine (pthreads). Can be empty,
   * see {@link FunctionEntryNode#getExitNode()}.
   */
  public final FunctionExitNode exitNode;

  /** The (sub)set of CFANodes from the original input CFA that this thread can reach. */
  public final ImmutableSet<ThreadNode> threadNodes;

  public final ImmutableSet<ThreadEdge> threadEdges;

  // TODO for each FunctionCallEdge, map the original CVariableDeclaration to the
  //  CParameterDeclaration. if a parameter declaration is a key, search for it in the values and
  //  replace the parameter declaration

  protected ThreadCFA(
      FunctionEntryNode pEntryNode,
      FunctionExitNode pExitNode,
      ImmutableSet<ThreadNode> pThreadNodes,
      ImmutableSet<ThreadEdge> pThreadEdges) {

    entryNode = pEntryNode;
    exitNode = pExitNode;
    threadNodes = pThreadNodes;
    threadEdges = pThreadEdges;
    initPredecessors();
    initSuccessors();
  }

  private void initPredecessors() {
    for (ThreadNode threadNode : threadNodes) {
      for (ThreadEdge threadEdge : threadNode.leavingEdges) {
        threadEdge.setPredecessor(threadNode);
      }
    }
  }

  private void initSuccessors() {
    for (ThreadEdge threadEdge : threadEdges) {
      threadEdge.setSuccessor(getThreadNodeByCfaNode(threadEdge.cfaEdge.getSuccessor()));
    }
  }

  private ThreadNode getThreadNodeByCfaNode(CFANode pCfaNode) {
    for (ThreadNode rThreadNode : threadNodes) {
      if (rThreadNode.cfaNode.equals(pCfaNode)) {
        return rThreadNode;
      }
    }
    throw new IllegalArgumentException("no corresponding ThreadNode found");
  }
}
