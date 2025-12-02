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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;

public class CFAForThread {

  private int nextFreePc;

  public int getNextFreePc() {
    return nextFreePc++;
  }

  public final int threadId;

  /** FunctionEntryNode of the main function (main thread) or start_routine (pthreads). */
  public final FunctionEntryNode entryNode;

  /**
   * FunctionExitNode of the main function (main thread) or start_routine (pthreads). Can be empty,
   * see {@link FunctionEntryNode#getExitNode()}.
   */
  public final Optional<FunctionExitNode> exitNode;

  /** The (sub)set of CFANodes from the original input CFA that this thread can reach. */
  public final ImmutableList<CFANodeForThread> threadNodes;

  public final ImmutableList<CFAEdgeForThread> threadEdges;

  protected CFAForThread(
      int pThreadId,
      FunctionEntryNode pEntryNode,
      ImmutableList<CFANodeForThread> pThreadNodes,
      ImmutableList<CFAEdgeForThread> pThreadEdges) {

    threadId = pThreadId;
    entryNode = pEntryNode;
    exitNode = entryNode.getExitNode();
    threadNodes = pThreadNodes;
    threadEdges = pThreadEdges;
    initPredecessors(threadNodes);
    initSuccessors(threadEdges, threadNodes);
    handleFunctionReturnEdges();
    nextFreePc = MPORThreadUtil.getHighestPc(threadNodes) + 1;
  }

  /**
   * Initializes successors of CFunctionReturnEdges or prunes them if they lead to another thread.
   */
  private void handleFunctionReturnEdges() {
    for (CFANodeForThread threadNode : threadNodes) {
      if (threadNode.cfaNode instanceof FunctionExitNode) {
        Set<CFAEdgeForThread> prunedEdges = new HashSet<>();
        for (CFAEdgeForThread threadEdge : threadNode.leavingEdges()) {
          Optional<CFANodeForThread> returnThreadNode =
              getReturnThreadNodeByCallContext(
                  threadEdge.cfaEdge.getSuccessor(), threadEdge.callContext);
          if (returnThreadNode.isPresent()) {
            threadEdge.setSuccessor(returnThreadNode.orElseThrow());
          } else {
            prunedEdges.add(threadEdge);
          }
        }
        for (CFAEdgeForThread prunedEdge : prunedEdges) {
          pruneEdge(prunedEdge);
        }
      }
    }
  }

  private void pruneEdge(CFAEdgeForThread pThreadEdge) {
    checkArgument(threadEdges.contains(pThreadEdge), "pThreadEdge not in threadEdges");
    checkArgument(
        pThreadEdge.cfaEdge instanceof CFunctionReturnEdge,
        "only CFunctionReturnEdges can be pruned");

    for (CFANodeForThread threadNode : threadNodes) {
      if (threadNode.leavingEdges().contains(pThreadEdge)) {
        threadNode.pruneLeavingEdge(pThreadEdge);
      }
    }
  }

  private static void initPredecessors(ImmutableList<CFANodeForThread> pThreadNodes) {
    for (CFANodeForThread threadNode : pThreadNodes) {
      for (CFAEdgeForThread threadEdge : threadNode.leavingEdges()) {
        threadEdge.setPredecessor(threadNode);
      }
    }
  }

  /**
   * Initializes all successor ThreadNodes for each ThreadEdge except CFunctionReturnEdges which are
   * handled in {@link CFAForThread#handleFunctionReturnEdges()}.
   */
  private static void initSuccessors(
      ImmutableList<CFAEdgeForThread> pThreadEdges, ImmutableList<CFANodeForThread> pThreadNodes) {

    outerLoop:
    for (CFAEdgeForThread threadEdge : pThreadEdges) {
      CFAEdge cfaEdge = threadEdge.cfaEdge;
      if (!(cfaEdge instanceof CFunctionReturnEdge)) {
        for (CFANodeForThread threadNode : pThreadNodes) {
          // check if predecessor node of edge is same as node
          if (threadNode.cfaNode.equals(cfaEdge.getSuccessor())) {
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

  private Optional<CFANodeForThread> getReturnThreadNodeByCallContext(
      CFANode pCfaNode, Optional<CFAEdgeForThread> pCallContext) {

    // no call context -> return the single thread node corresponding to pCfaNode
    if (pCallContext.isEmpty()) {
      ImmutableSet<CFANodeForThread> threadNodesByCfaNode = getThreadNodesByCfaNode(pCfaNode);
      assert threadNodesByCfaNode.size() == 1
          : "if there is no calling context, the cfa node can have only one corresponding thread"
              + " node";
      return Optional.of(threadNodesByCfaNode.iterator().next());
    }

    // otherwise extract return node based on call context
    CFANodeForThread callNode = pCallContext.orElseThrow().getPredecessor();
    for (CFAEdgeForThread threadEdge : callNode.leavingEdges()) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge functionSummaryEdge) {
        return getThreadNodeByCfaNodeAndCallContext(
            functionSummaryEdge.getSuccessor(), threadEdge.callContext);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns all {@link CFANodeForThread}s associated with {@code pCfaNode}, one for each call
   * context.
   */
  private ImmutableSet<CFANodeForThread> getThreadNodesByCfaNode(CFANode pCfaNode) {
    return threadNodes.stream()
        .filter(threadNode -> threadNode.cfaNode.equals(pCfaNode))
        .collect(ImmutableSet.toImmutableSet());
  }

  private Optional<CFANodeForThread> getThreadNodeByCfaNodeAndCallContext(
      CFANode pCfaNode, Optional<CFAEdgeForThread> pCallContext) {

    for (CFANodeForThread threadNode : threadNodes) {
      if (threadNode.cfaNode.equals(pCfaNode)) {
        if (threadNode.callContext.equals(pCallContext)) {
          return Optional.of(threadNode);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public int hashCode() {
    return Objects.hash(threadId, entryNode, exitNode, threadNodes, threadEdges);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof CFAForThread other
        && threadId == other.threadId
        && entryNode.equals(other.entryNode)
        && exitNode.equals(other.exitNode)
        && threadNodes.equals(other.threadNodes)
        && threadEdges.equals(other.threadEdges);
  }
}
