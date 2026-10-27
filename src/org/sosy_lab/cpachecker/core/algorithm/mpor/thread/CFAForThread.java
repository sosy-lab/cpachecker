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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
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

  /** The (sub)set of CFANodes from the original input CFA that this thread can reach. */
  public final ImmutableList<CFANodeForThread> threadNodes;

  /** The (sub)set of CFANode loop heads from the original input CFA that this thread can reach. */
  private final ImmutableSet<CFANodeForThread> loopHeads;

  public final ImmutableList<CFAEdgeForThread> threadEdges;

  CFAForThread(
      int pThreadId,
      FunctionEntryNode pEntryNode,
      ImmutableList<CFANodeForThread> pInitialNodes,
      ImmutableSet<CFANodeForThread> pLoopHeads,
      ImmutableList<CFAEdgeForThread> pInitialEdges,
      ImmutableMap<SeqCallContext, CFAEdgeForThread> pVisitedInitRoutines) {

    checkArgument(
        pInitialNodes.stream().anyMatch(n -> n.getCfaNode().equals(pEntryNode)),
        "pEntryNode must be present in pInitialNodes.");
    checkArgument(
        pInitialNodes.containsAll(pLoopHeads), "All pLoopHeads must be present in pInitialNodes.");

    threadId = pThreadId;
    entryNode = pEntryNode;
    loopHeads = pLoopHeads;

    initPredecessors(pInitialNodes);
    initSuccessors(pInitialEdges, pInitialNodes);

    List<CFANodeForThread> finalNodes = new ArrayList<>();
    List<CFAEdgeForThread> finalEdges = new ArrayList<>();

    handleFunctionReturnEdges(
        pThreadId, pInitialNodes, pInitialEdges, finalNodes, finalEdges, pVisitedInitRoutines);

    threadNodes = ImmutableList.copyOf(finalNodes);
    threadEdges = ImmutableList.copyOf(finalEdges);

    nextFreePc = MPORThreadUtil.getHighestPc(threadNodes) + 1;
  }

  /**
   * Initializes {@link FunctionReturnEdge}s and their successors or prunes them if they lead to
   * another thread, and collects the updated nodes and edges.
   */
  private static void handleFunctionReturnEdges(
      int pThreadId,
      ImmutableList<CFANodeForThread> pInitialNodes,
      ImmutableList<CFAEdgeForThread> pInitialEdges,
      List<CFANodeForThread> pFinalNodes,
      List<CFAEdgeForThread> pFinalEdges,
      ImmutableMap<SeqCallContext, CFAEdgeForThread> pVisitedInitRoutines) {

    for (CFANodeForThread threadNode : pInitialNodes) {
      List<CFAEdgeForThread> finalEdges = new ArrayList<>(threadNode.leavingEdges());

      if (threadNode.getCfaNode() instanceof FunctionExitNode functionExitNode) {
        // Add the missing FunctionReturnEdge after entering the init_routine from pthread_once.
        if (threadNode.leavingEdges().isEmpty()
            && pVisitedInitRoutines.containsKey(threadNode.callContext)) {
          CFAEdgeForThread functionSummaryThreadEdge =
              pVisitedInitRoutines.get(threadNode.callContext);
          CFunctionSummaryEdge functionSummaryEdge =
              (CFunctionSummaryEdge) Objects.requireNonNull(functionSummaryThreadEdge).cfaEdge;

          if (functionSummaryEdge.getFunctionEntry().equals(functionExitNode.getEntryNode())) {
            FunctionReturnEdge functionReturnEdge =
                new FunctionReturnEdge(
                    FileLocation.DUMMY,
                    functionExitNode,
                    functionSummaryEdge.getSuccessor(),
                    functionSummaryEdge);

            CFAEdgeForThread functionReturnThreadEdge =
                new CFAEdgeForThread(pThreadId, functionReturnEdge, threadNode.callContext);
            functionReturnThreadEdge.setSuccessor(functionSummaryThreadEdge.getSuccessor());
            functionReturnThreadEdge.setPredecessor(threadNode);

            finalEdges.add(functionReturnThreadEdge);
          }
        } else {
          for (CFAEdgeForThread threadEdge : threadNode.leavingEdges()) {
            Optional<CFANodeForThread> returnThreadNode =
                getReturnThreadNodeByCallContext(
                    threadEdge.cfaEdge.getSuccessor(), threadEdge.callContext, pInitialNodes);

            if (returnThreadNode.isPresent()) {
              threadEdge.setSuccessor(returnThreadNode.orElseThrow());
            } else {
              finalEdges.remove(threadEdge);
              pruneEdgeFromAllPredecessors(threadEdge, pInitialNodes, pInitialEdges);
            }
          }
        }
      }

      if (!finalEdges.equals(threadNode.leavingEdges())) {
        pFinalNodes.add(threadNode.withLeavingEdges(ImmutableList.copyOf(finalEdges)));
      } else {
        pFinalNodes.add(threadNode);
      }
      pFinalEdges.addAll(finalEdges);
    }
  }

  private static void pruneEdgeFromAllPredecessors(
      CFAEdgeForThread pThreadEdge,
      ImmutableList<CFANodeForThread> pThreadNodes,
      ImmutableList<CFAEdgeForThread> pThreadEdges) {

    checkArgument(pThreadEdges.contains(pThreadEdge), "pThreadEdge not in threadEdges");
    checkArgument(
        pThreadEdge.cfaEdge instanceof CFunctionReturnEdge,
        "only CFunctionReturnEdges can be pruned");

    for (CFANodeForThread threadNode : pThreadNodes) {
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
   * handled in {@link CFAForThread#handleFunctionReturnEdges(int, ImmutableList, ImmutableList,
   * Map)}.
   */
  private static void initSuccessors(
      ImmutableList<CFAEdgeForThread> pThreadEdges, ImmutableList<CFANodeForThread> pThreadNodes) {

    outerLoop:
    for (CFAEdgeForThread threadEdge : pThreadEdges) {
      CFAEdge cfaEdge = threadEdge.cfaEdge;
      if (!(cfaEdge instanceof CFunctionReturnEdge)) {
        for (CFANodeForThread threadNode : pThreadNodes) {
          // check if predecessor node of edge is same as node
          if (threadNode.getCfaNode().equals(cfaEdge.getSuccessor())) {
            // check if calling context for node and edge are the same
            if (cfaEdge instanceof CFunctionCallEdge) {
              // for call edges, we use the edge as the call context
              if (threadNode.callContext.equals(new SeqCallContext(Optional.of(threadEdge)))) {
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

  private static Optional<CFANodeForThread> getReturnThreadNodeByCallContext(
      CFANode pCfaNode, SeqCallContext pCallContext, ImmutableList<CFANodeForThread> pThreadNodes) {

    // no call context -> return the single thread node corresponding to pCfaNode
    if (pCallContext.cfaEdgeForThread().isEmpty()) {
      ImmutableSet<CFANodeForThread> threadNodesByCfaNode =
          getThreadNodesByCfaNode(pCfaNode, pThreadNodes);
      return Optional.of(Iterables.getOnlyElement(threadNodesByCfaNode));
    }

    // otherwise extract return node based on call context
    CFANodeForThread callNode = pCallContext.cfaEdgeForThread().orElseThrow().getPredecessor();
    for (CFAEdgeForThread threadEdge : callNode.leavingEdges()) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge functionSummaryEdge) {
        return getThreadNodeByCfaNodeAndCallContext(
            functionSummaryEdge.getSuccessor(), threadEdge.callContext, pThreadNodes);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns all {@link CFANodeForThread}s associated with {@code pCfaNode}, one for each call
   * context.
   */
  private static ImmutableSet<CFANodeForThread> getThreadNodesByCfaNode(
      CFANode pCfaNode, ImmutableList<CFANodeForThread> pThreadNodes) {
    return pThreadNodes.stream()
        .filter(threadNode -> threadNode.getCfaNode().equals(pCfaNode))
        .collect(ImmutableSet.toImmutableSet());
  }

  private static Optional<CFANodeForThread> getThreadNodeByCfaNodeAndCallContext(
      CFANode pCfaNode, SeqCallContext pCallContext, ImmutableList<CFANodeForThread> pThreadNodes) {

    for (CFANodeForThread threadNode : pThreadNodes) {
      if (threadNode.getCfaNode().equals(pCfaNode)) {
        if (threadNode.callContext.equals(pCallContext)) {
          return Optional.of(threadNode);
        }
      }
    }
    return Optional.empty();
  }

  public ImmutableSet<CFANodeForThread> getLoopHeads() {
    return loopHeads;
  }

  @Override
  public int hashCode() {
    return Objects.hash(threadId, entryNode, threadNodes, threadEdges);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof CFAForThread other
        && threadId == other.threadId
        && entryNode.equals(other.entryNode)
        && threadNodes.equals(other.threadNodes)
        && threadEdges.equals(other.threadEdges);
  }
}
