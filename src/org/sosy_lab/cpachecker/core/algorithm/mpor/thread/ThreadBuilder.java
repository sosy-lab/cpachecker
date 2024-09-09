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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order.MPORCreate;
import org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order.MPORJoin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order.MPORMutex;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.util.CFAUtils;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class ThreadBuilder {

  private int currentId = 0;

  private int currentPc = SeqUtil.INIT_PC;

  /** A copy of the GlobalAccessChecker in {@link MPORAlgorithm}. */
  private final GlobalAccessChecker GAC;

  /** A copy of the functionCallMap in {@link MPORAlgorithm}. */
  private final ImmutableMap<CFANode, CFANode> functionCallMap;

  public ThreadBuilder(GlobalAccessChecker pGac, ImmutableMap<CFANode, CFANode> pFunctionCallMap) {
    GAC = pGac;
    functionCallMap = pFunctionCallMap;
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   *
   * @param pThreadObject the pthread_t object, set to empty for the main thread
   * @param pEntryNode the entry node of the start routine or main function of the thread
   * @param pExitNode the exit node of the start routine or main function of the thread
   * @return a MPORThread object with properly initialized variables
   */
  public MPORThread createThread(
      Optional<CExpression> pThreadObject,
      FunctionEntryNode pEntryNode,
      FunctionExitNode pExitNode) {

    currentPc = SeqUtil.INIT_PC; // reset pc for every thread created

    Set<CFANode> visitedNodes = new HashSet<>(); // using set so that we can use .contains()
    ImmutableSet.Builder<ThreadNode> threadNodes = ImmutableSet.builder();
    ImmutableSet.Builder<ThreadEdge> threadEdges = ImmutableSet.builder();
    initThreadVariables(
        pExitNode, visitedNodes, threadNodes, threadEdges, pEntryNode, Optional.empty());

    ThreadCFA threadCfa =
        new ThreadCFA(pEntryNode, pExitNode, threadNodes.build(), threadEdges.build());

    ImmutableSet<CVariableDeclaration> localVars = getLocalVars(threadEdges.build());

    ImmutableSet.Builder<MPORCreate> creates = ImmutableSet.builder();
    searchThreadForCreates(
        creates, pExitNode, new HashSet<>(), new HashSet<>(), pEntryNode, Optional.empty());

    ImmutableSet.Builder<MPORMutex> mutexes = ImmutableSet.builder();
    searchThreadForMutexes(mutexes, pExitNode, new HashSet<>(), pEntryNode, Optional.empty());

    ImmutableSet.Builder<MPORJoin> joins = ImmutableSet.builder();
    searchThreadForJoins(joins, pExitNode, new HashSet<>(), pEntryNode, Optional.empty());

    // TODO searchThreadForBarriers

    return new MPORThread(
        currentId++,
        pThreadObject,
        localVars,
        creates.build(),
        mutexes.build(),
        joins.build(),
        threadCfa);
  }

  /**
   * Recursively searches the CFA of a thread specified by its entry node (the first pCurrentNode)
   * and pExitNode.
   *
   * @param pExitNode the FunctionExitNode of the start routine or main function of the thread
   * @param pVisitedNodes the set of already visited CFANodes
   * @param pThreadNodes the set of ThreadNodes reachable by the thread
   * @param pCurrentNode the current CFANode whose leaving CFAEdges are analyzed
   * @param pFuncReturnNode pFuncReturnNode used to track the original context when entering the CFA
   *     of another function.
   */
  private void initThreadVariables(
      final FunctionExitNode pExitNode,
      Set<CFANode> pVisitedNodes,
      ImmutableSet.Builder<ThreadNode> pThreadNodes,
      ImmutableSet.Builder<ThreadEdge> pThreadEdges,
      CFANode pCurrentNode,
      Optional<CFANode> pFuncReturnNode) {

    if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
      ImmutableSet<CFAEdge> leavingCfaEdges =
          MPORUtil.allReturnLeavingEdges(pCurrentNode, pFuncReturnNode);
      ImmutableSet<ThreadEdge> threadEdges = createThreadEdgesFromCfaEdges(leavingCfaEdges);
      pThreadEdges.addAll(threadEdges);
      if (leavingCfaEdges.isEmpty()) {
        pThreadNodes.add(new ThreadNode(pCurrentNode, SeqUtil.EXIT_PC, threadEdges));
      } else {
        pThreadNodes.add(new ThreadNode(pCurrentNode, currentPc++, threadEdges));
        for (CFAEdge cfaEdge : leavingCfaEdges) {
          initThreadVariables(
              pExitNode,
              pVisitedNodes,
              pThreadNodes,
              pThreadEdges,
              cfaEdge.getSuccessor(),
              updateFuncReturnNode(pCurrentNode, pFuncReturnNode));
        }
      }
    }
  }

  /** Extracts all local variable declarations from pThreadEdges. */
  private ImmutableSet<CVariableDeclaration> getLocalVars(ImmutableSet<ThreadEdge> pThreadEdges) {
    ImmutableSet.Builder<CVariableDeclaration> rLocalVars = ImmutableSet.builder();
    for (ThreadEdge threadEdge : pThreadEdges) {
      CFAEdge edge = threadEdge.cfaEdge;
      if (edge instanceof CDeclarationEdge declarationEdge) {
        if (!GAC.hasGlobalAccess(edge) && !declarationEdge.getDeclaration().isGlobal()) {
          AAstNode aAstNode = declarationEdge.getRawAST().orElseThrow();
          // exclude FunctionDeclarations
          if (aAstNode instanceof CVariableDeclaration cVariableDeclaration) {
            rLocalVars.add(cVariableDeclaration);
          }
        }
      }
    }
    return rLocalVars.build();
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
          if (PthreadFuncType.isEdgeCallToFuncType(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
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
          if (PthreadFuncType.isEdgeCallToFuncType(cfaEdge, PthreadFuncType.PTHREAD_MUTEX_LOCK)) {
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
        if (PthreadFuncType.isEdgeCallToFuncType(cfaEdge, PthreadFuncType.PTHREAD_MUTEX_UNLOCK)) {
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
          if (PthreadFuncType.isEdgeCallToFuncType(cfaEdge, PthreadFuncType.PTHREAD_JOIN)) {
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
    if (!pCurrentNode.equals(pThread.cfa.exitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : MPORUtil.returnLeavingEdges(pCurrentNode, pFuncReturnNode)) {
          if (PthreadFuncType.isEdgeCallToFuncType(cfaEdge, PthreadFuncType.BARRIER_INIT)) {
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

  private ImmutableSet<ThreadEdge> createThreadEdgesFromCfaEdges(ImmutableSet<CFAEdge> pCfaEdges) {
    ImmutableSet.Builder<ThreadEdge> rThreadEdges = ImmutableSet.builder();
    for (CFAEdge cfaEdge : pCfaEdges) {
      rThreadEdges.add(new ThreadEdge(cfaEdge));
    }
    return rThreadEdges.build();
  }
}
