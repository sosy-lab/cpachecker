// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.FunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORCreate;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORJoin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORMutex;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ThreadBuilder {

  /** A copy of the functionCallMap in {@link MPORAlgorithm}. */
  private final ImmutableMap<CFANode, CFANode> functionCallMap;

  public ThreadBuilder(ImmutableMap<CFANode, CFANode> pFunctionCallMap) {
    functionCallMap = pFunctionCallMap;
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   *
   * @param pPthreadT the pthread_t object, set to empty for the main thread
   * @param pEntryNode the entry node of the start routine or main function of the thread
   * @param pExitNode the exit node of the start routine or main function of the thread
   * @return a MPORThread object with properly initialized variables
   */
  public MPORThread createThread(
      Optional<CExpression> pPthreadT, FunctionEntryNode pEntryNode, FunctionExitNode pExitNode) {

    Set<CFANode> threadNodes = new HashSet<>(); // using set so that we can use .contains()
    ImmutableSet.Builder<CFAEdge> threadEdges = ImmutableSet.builder();
    initThreadVariables(pExitNode, threadNodes, threadEdges, pEntryNode, Optional.empty());

    ImmutableSet.Builder<MPORCreate> creates = ImmutableSet.builder();
    searchThreadForCreates(
        creates, pExitNode, new HashSet<>(), new HashSet<>(), pEntryNode, Optional.empty());

    ImmutableSet.Builder<MPORMutex> mutexes = ImmutableSet.builder();
    searchThreadForMutexes(mutexes, pExitNode, new HashSet<>(), pEntryNode, Optional.empty());

    ImmutableSet.Builder<MPORJoin> joins = ImmutableSet.builder();
    searchThreadForJoins(joins, pExitNode, new HashSet<>(), pEntryNode, Optional.empty());

    // TODO searchThreadForBarriers

    return new MPORThread(
        pPthreadT,
        pEntryNode,
        pExitNode,
        ImmutableSet.copyOf(threadNodes),
        threadEdges.build(),
        creates.build(),
        mutexes.build(),
        joins.build());
  }

  /**
   * Recursively searches the CFA of a thread specified by its entry node (the first pCurrentNode)
   * and pExitNode.
   *
   * @param pExitNode the FunctionExitNode of the start routine or main function of the thread
   * @param pThreadNodes the set of already visited CFANodes that are inside the thread
   * @param pThreadEdges the set of CFAEdges executed by the thread
   * @param pCurrentNode the current CFANode whose leaving CFAEdges are analyzed
   * @param pFuncReturnNode pFuncReturnNode used to track the original context when entering the CFA
   *     of another function.
   */
  private void initThreadVariables(
      final FunctionExitNode pExitNode,
      Set<CFANode> pThreadNodes, // set so that we can use .contains
      ImmutableSet.Builder<CFAEdge> pThreadEdges,
      CFANode pCurrentNode,
      Optional<CFANode> pFuncReturnNode) {

    if (MPORUtil.shouldVisit(pThreadNodes, pCurrentNode)) {
      if (!pCurrentNode.equals(pExitNode)) {
        for (CFAEdge cfaEdge : MPORUtil.returnLeavingEdges(pCurrentNode, pFuncReturnNode)) {
          pThreadEdges.add(cfaEdge);
          initThreadVariables(
              pExitNode,
              pThreadNodes,
              pThreadEdges,
              cfaEdge.getSuccessor(),
              updateFuncReturnNode(pCurrentNode, pFuncReturnNode));
        }
      }
    }
  }

  /**
   * Recursively searches the CFA of the thread with pThreadExitNode for pthread_create calls.
   *
   * @param pCreates the set of creates to be created
   * @param pThreadExitNode the exit node of the thread (the FunctionExitNode of the start routine)
   * @param pVisitedNodes keep track of already visited CFANodes to prevent an infinite loop if
   *     there are loop structures in the CFA
   * @param pEdgesTrace trace CFAEdges executed until pthread_create is encountered
   * @param pCurrentNode the CFANode whose leaving CFAEdges we analyze for pthread_create
   * @param pFuncReturnNode used to track the original context when entering the CFA of another
   *     function.
   */
  private void searchThreadForCreates(
      ImmutableSet.Builder<MPORCreate> pCreates,
      final CFANode pThreadExitNode,
      Set<CFANode> pVisitedNodes,
      Set<CFAEdge> pEdgesTrace,
      CFANode pCurrentNode,
      Optional<CFANode> pFuncReturnNode) {

    if (!pCurrentNode.equals(pThreadExitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : MPORUtil.returnLeavingEdges(pCurrentNode, pFuncReturnNode)) {
          if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_CREATE)) {
            pEdgesTrace.add(cfaEdge);
            CExpression pthreadT =
                CFAUtils.getValueFromPointer(CFAUtils.getParameterAtIndex(cfaEdge, 0));
            pCreates.add(new MPORCreate(pthreadT, ImmutableSet.copyOf(pEdgesTrace)));
          }
          pEdgesTrace.add(cfaEdge);
          searchThreadForCreates(
              pCreates,
              pThreadExitNode,
              pVisitedNodes,
              // copying set so that it does not contain edges from a "parallel" trace (i.e. nondet)
              new HashSet<>(pEdgesTrace),
              cfaEdge.getSuccessor(),
              updateFuncReturnNode(pCurrentNode, pFuncReturnNode));
        }
      }
    }
  }

  /**
   * Recursively searches the CFA of pThread for mutex_locks.
   *
   * @param pMutexes the set of mutexes to be created
   * @param pThreadExitNode the exit node of the thread (the FunctionExitNode of the start routine)
   * @param pVisitedNodes keep track of already visited CFANodes to prevent an infinite loop if
   *     there are loop structures in the CFA
   * @param pCurrentNode the CFANode whose leaving CFAEdges we analyze for mutex_locks
   * @param pFuncReturnNode used to track the original context when entering the CFA of another
   *     function.
   */
  private void searchThreadForMutexes(
      ImmutableSet.Builder<MPORMutex> pMutexes,
      final CFANode pThreadExitNode,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      Optional<CFANode> pFuncReturnNode) {

    if (!pCurrentNode.equals(pThreadExitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : MPORUtil.returnLeavingEdges(pCurrentNode, pFuncReturnNode)) {
          if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_MUTEX_LOCK)) {
            CExpression pthreadMutexT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
            // the successor node of mutex_lock is the first inside the lock
            CFANode initialNode = cfaEdge.getSuccessor();
            Set<CFANode> mutexNodes = new HashSet<>(); // using a set so that we can use .contains
            ImmutableSet.Builder<CFAEdge> mutexEdges = ImmutableSet.builder();
            ImmutableSet.Builder<CFANode> mutexExitNodes = ImmutableSet.builder();
            initMutexVariables(
                pthreadMutexT,
                mutexNodes,
                mutexEdges,
                mutexExitNodes,
                initialNode,
                Optional.empty());
            MPORMutex mutex =
                new MPORMutex(
                    pthreadMutexT,
                    initialNode,
                    ImmutableSet.copyOf(mutexNodes),
                    mutexEdges.build(),
                    mutexExitNodes.build());
            pMutexes.add(mutex);
          }
          searchThreadForMutexes(
              pMutexes,
              pThreadExitNode,
              pVisitedNodes,
              cfaEdge.getSuccessor(),
              updateFuncReturnNode(pCurrentNode, pFuncReturnNode));
        }
      }
    }
  }

  /**
   * Recursively searches the CFA of pThread for all (entry and exit) CFANodes and CFAEdges inside
   * the mutex lock.
   *
   * @param pPthreadMutexT the pthread_mutex_t object encountered in a pthread_mutex_lock call
   * @param pMutexNodes the set of CFANodes inside the mutex lock, made immutable once finished
   * @param pMutexEdges the set of CFAEdges inside the mutex lock
   * @param pMutexExitNodes the set of CFANodes whose leaving edge(s) are pthread_mutex_unlocks
   * @param pCurrentNode the current CFANode whose leaving Edges we search for mutex_unlocks
   * @param pFuncReturnNode used to track the original context when entering the CFA of another
   *     function.
   */
  private void initMutexVariables(
      final CExpression pPthreadMutexT,
      Set<CFANode> pMutexNodes, // using a set so that we can use .contains(...)
      ImmutableSet.Builder<CFAEdge> pMutexEdges,
      ImmutableSet.Builder<CFANode> pMutexExitNodes,
      CFANode pCurrentNode,
      Optional<CFANode> pFuncReturnNode) {

    // visit CFANodes only once to prevent infinite loops in case of loop structures
    if (MPORUtil.shouldVisit(pMutexNodes, pCurrentNode)) {
      for (CFAEdge cfaEdge : MPORUtil.returnLeavingEdges(pCurrentNode, pFuncReturnNode)) {
        pMutexEdges.add(cfaEdge);
        if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_MUTEX_UNLOCK)) {
          CExpression pthreadMutexT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
          if (pthreadMutexT.equals(pPthreadMutexT)) {
            pMutexExitNodes.add(pCurrentNode);
          }
        } else {
          initMutexVariables(
              pPthreadMutexT,
              pMutexNodes,
              pMutexEdges,
              pMutexExitNodes,
              cfaEdge.getSuccessor(),
              updateFuncReturnNode(pCurrentNode, pFuncReturnNode));
        }
      }
    }
  }

  /**
   * Recursively searches the CFA of pThread for pthread_join calls.
   *
   * @param pJoins the set of joins to be created
   * @param pThreadExitNode the FunctionExitNode of the threads start routine
   * @param pVisitedNodes keep track of already visited CFANodes to prevent an infinite loop if
   *     there are loop structures in the CFA
   * @param pCurrentNode the CFANode whose leaving CFAEdges we analyze for pthread_joins
   * @param pFuncReturnNode used to track the original context when entering the CFA of another
   *     function.
   */
  private void searchThreadForJoins(
      ImmutableSet.Builder<MPORJoin> pJoins,
      final CFANode pThreadExitNode,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      Optional<CFANode> pFuncReturnNode) {

    if (!pCurrentNode.equals(pThreadExitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : MPORUtil.returnLeavingEdges(pCurrentNode, pFuncReturnNode)) {
          if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_JOIN)) {
            CExpression pthreadT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
            MPORJoin join = new MPORJoin(pthreadT, pCurrentNode, cfaEdge);
            pJoins.add(join);
          }
          searchThreadForJoins(
              pJoins,
              pThreadExitNode,
              pVisitedNodes,
              cfaEdge.getSuccessor(),
              updateFuncReturnNode(pCurrentNode, pFuncReturnNode));
        }
      }
    }
  }

  /**
   * TODO
   *
   * @param pThread TODO
   * @param pVisitedNodes TODO
   * @param pCurrentNode TODO
   * @param pFuncReturnNode TODO
   */
  private void searchThreadForBarriers(
      MPORThread pThread,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      Optional<CFANode> pFuncReturnNode) {

    // TODO see pthread-divine for example barrier programs
    if (!pCurrentNode.equals(pThread.exitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : MPORUtil.returnLeavingEdges(pCurrentNode, pFuncReturnNode)) {
          if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.BARRIER_INIT)) {
            // TODO unsure how to handle this, SV benchmarks use their custom barrier objects and
            //  functions, e.g. pthread-divine/barrier_2t.i
            //  but the general approach is identifying MPORBarriers from barrier_init calls
            //  and then identify the corresponding MPORBarrierWaits
          }
          searchThreadForBarriers(
              pThread,
              pVisitedNodes,
              cfaEdge.getSuccessor(),
              updateFuncReturnNode(pCurrentNode, pFuncReturnNode));
        }
      }
    }
  }

  // (Private) Helpers =============================================================================

  private Optional<CFANode> updateFuncReturnNode(
      CFANode pCurrentNode, Optional<CFANode> pPrevFuncReturnNode) {
    return MPORUtil.updateFuncReturnNode(functionCallMap, pCurrentNode, pPrevFuncReturnNode);
  }
}