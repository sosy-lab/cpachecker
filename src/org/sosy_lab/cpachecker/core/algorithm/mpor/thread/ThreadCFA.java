// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;

public class ThreadCFA {

  /** FunctionEntryNode of the main function (main thread) or start routine (pthreads). */
  public final FunctionEntryNode entryNode;

  /**
   * FunctionExitNode of the main function (main thread) or start routine (pthreads). Can be empty,
   * see {@link FunctionEntryNode#getExitNode()}.
   */
  public final Optional<FunctionExitNode> exitNode;

  /** The (sub)set of CFANodes from the original input CFA that this thread can reach. */
  public final ImmutableSet<ThreadNode> threadNodes;

  public final ImmutableSet<ThreadEdge> threadEdges;

  public final ImmutableSet<CFunctionDeclaration> calledFuncs;

  protected ThreadCFA(
      FunctionEntryNode pEntryNode,
      ImmutableSet<ThreadNode> pThreadNodes,
      ImmutableSet<ThreadEdge> pThreadEdges,
      ImmutableSet<CFunctionDeclaration> pCalledFuncs) {

    entryNode = pEntryNode;
    exitNode = entryNode.getExitNode();
    threadNodes = pThreadNodes;
    threadEdges = pThreadEdges;
    calledFuncs = pCalledFuncs;
    initPredecessors();
    initSuccessors();
    handleFuncReturnEdges();
  }

  /**
   * Initializes successors of CFunctionReturnEdges or prunes them if they lead to another thread.
   */
  private void handleFuncReturnEdges() {
    for (ThreadNode threadNode : threadNodes) {
      if (threadNode.cfaNode instanceof FunctionExitNode) {
        Set<ThreadEdge> prunedEdges = new HashSet<>();
        for (ThreadEdge threadEdge : threadNode.leavingEdges()) {
          ThreadNode successor = getThreadNodeByCfaNode(threadEdge.cfaEdge.getSuccessor());
          if (successor == null) {
            prunedEdges.add(threadEdge);
          } else {
            threadEdge.setSuccessor(successor);
          }
        }
        for (ThreadEdge prunedEdge : prunedEdges) {
          pruneEdge(prunedEdge);
        }
      }
    }
  }

  private void pruneEdge(ThreadEdge pThreadEdge) {
    checkArgument(threadEdges.contains(pThreadEdge), "pThreadEdge not in threadEdges");
    checkArgument(
        pThreadEdge.cfaEdge instanceof CFunctionReturnEdge,
        "only CFunctionReturnEdges can be pruned");

    for (ThreadNode threadNode : threadNodes) {
      if (threadNode.leavingEdges().contains(pThreadEdge)) {
        threadNode.pruneLeavingEdge(pThreadEdge);
      }
    }
  }

  private void initPredecessors() {
    for (ThreadNode threadNode : threadNodes) {
      for (ThreadEdge threadEdge : threadNode.leavingEdges()) {
        threadEdge.setPredecessor(threadNode);
      }
    }
  }

  /**
   * Initializes all successor ThreadNodes for each ThreadEdge except CFunctionReturnEdges which are
   * handled in {@link ThreadCFA#handleFuncReturnEdges()}.
   */
  private void initSuccessors() {
    for (ThreadEdge threadEdge : threadEdges) {
      if (!(threadEdge.cfaEdge instanceof CFunctionReturnEdge)) {
        ThreadNode successor = getThreadNodeByCfaNode(threadEdge.cfaEdge.getSuccessor());
        threadEdge.setSuccessor(successor);
      }
    }
  }

  public ThreadNode getThreadNodeByCfaNode(CFANode pCfaNode) {
    for (ThreadNode rThreadNode : threadNodes) {
      if (rThreadNode.cfaNode.equals(pCfaNode)) {
        return rThreadNode;
      }
    }
    return null;
  }
}
