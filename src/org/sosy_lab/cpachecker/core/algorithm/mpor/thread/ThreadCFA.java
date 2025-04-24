// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;

public class ThreadCFA {

  public final int threadId;

  /** FunctionEntryNode of the main function (main thread) or start_routine (pthreads). */
  public final FunctionEntryNode entryNode;

  /**
   * FunctionExitNode of the main function (main thread) or start_routine (pthreads). Can be empty,
   * see {@link FunctionEntryNode#getExitNode()}.
   */
  public final Optional<FunctionExitNode> exitNode;

  /** The (sub)set of CFANodes from the original input CFA that this thread can reach. */
  public final ImmutableSet<ThreadNode> threadNodes;

  public final ImmutableList<ThreadEdge> threadEdges;

  protected ThreadCFA(
      int pThreadId,
      FunctionEntryNode pEntryNode,
      ImmutableSet<ThreadNode> pThreadNodes,
      ImmutableList<ThreadEdge> pThreadEdges) {

    threadId = pThreadId;
    entryNode = pEntryNode;
    exitNode = entryNode.getExitNode();
    threadNodes = pThreadNodes;
    threadEdges = pThreadEdges;
    initPredecessors(threadNodes);
    initSuccessors(threadEdges, threadNodes);
    handleFunctionReturnEdges();
  }

  /**
   * Initializes successors of CFunctionReturnEdges or prunes them if they lead to another thread.
   */
  private void handleFunctionReturnEdges() {
    for (ThreadNode threadNode : threadNodes) {
      if (threadNode.cfaNode instanceof FunctionExitNode) {
        Set<ThreadEdge> prunedEdges = new HashSet<>();
        for (ThreadEdge threadEdge : threadNode.leavingEdges()) {
          Optional<ThreadNode> returnThreadNode =
              getReturnThreadNodeByCallContext(
                  threadEdge.cfaEdge.getSuccessor(), threadEdge.callContext);
          if (returnThreadNode.isPresent()) {
            threadEdge.setSuccessor(returnThreadNode.orElseThrow());
          } else {
            prunedEdges.add(threadEdge);
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

  private static void initPredecessors(ImmutableSet<ThreadNode> pThreadNodes) {
    for (ThreadNode threadNode : pThreadNodes) {
      for (ThreadEdge threadEdge : threadNode.leavingEdges()) {
        threadEdge.setPredecessor(threadNode);
      }
    }
  }

  /**
   * Initializes all successor ThreadNodes for each ThreadEdge except CFunctionReturnEdges which are
   * handled in {@link ThreadCFA#handleFunctionReturnEdges()}.
   */
  private static void initSuccessors(
      ImmutableList<ThreadEdge> pThreadEdges, ImmutableSet<ThreadNode> pThreadNodes) {

    outerLoop:
    for (ThreadEdge threadEdge : pThreadEdges) {
      CFAEdge cfaEdge = threadEdge.cfaEdge;
      if (!(cfaEdge instanceof CFunctionReturnEdge)) {
        CFANode edgeSuccessor = cfaEdge.getSuccessor();
        for (ThreadNode threadNode : pThreadNodes) {
          // check if predecessor node of edge is same as node
          if (threadNode.cfaNode.equals(edgeSuccessor)) {
            // check if calling context for node and edge are the same
            if (cfaEdge instanceof CFunctionCallEdge) {
              // for call edges, we use the edge as the call context
              if (threadNode.callContext.equals(Optional.of(threadEdge))) {
                threadEdge.setSuccessor(threadNode);
                continue outerLoop;
              }
            } else {
              // for all other edges, we use the edges respective call context
              if (threadNode.callContext.equals(threadEdge.callContext)) {
                threadEdge.setSuccessor(threadNode);
                continue outerLoop;
              }
            }
          }
        }
      }
    }
  }

  private Optional<ThreadNode> getReturnThreadNodeByCallContext(
      CFANode pCfaNode, Optional<ThreadEdge> pCallContext) {

    // no call context -> return the single thread node corresponding to pCfaNode
    if (pCallContext.isEmpty()) {
      ImmutableSet<ThreadNode> threadNodesByCfaNode = getThreadNodesByCfaNode(pCfaNode);
      assert threadNodesByCfaNode.size() == 1
          : "if there is no calling context, the cfa node can have only one corresponding thread"
              + " node";
      return Optional.of(threadNodesByCfaNode.iterator().next());
    }

    // otherwise extract return node based on call context
    ThreadNode callNode = pCallContext.orElseThrow().getPredecessor();
    for (ThreadEdge threadEdge : callNode.leavingEdges()) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge functionSummaryEdge) {
        return getThreadNodeByCfaNodeAndCallContext(
            functionSummaryEdge.getSuccessor(), threadEdge.callContext);
      }
    }
    return Optional.empty();
  }

  private ImmutableSet<ThreadNode> getThreadNodesByCfaNode(CFANode pCfaNode) {
    return threadNodes.stream()
        .filter(threadNode -> threadNode.cfaNode.equals(pCfaNode))
        .collect(ImmutableSet.toImmutableSet());
  }

  private Optional<ThreadNode> getThreadNodeByCfaNodeAndCallContext(
      CFANode pCfaNode, Optional<ThreadEdge> pCallContext) {

    for (ThreadNode threadNode : threadNodes) {
      if (threadNode.cfaNode.equals(pCfaNode)) {
        if (threadNode.callContext.equals(pCallContext)) {
          return Optional.of(threadNode);
        }
      }
    }
    return Optional.empty();
  }
}
