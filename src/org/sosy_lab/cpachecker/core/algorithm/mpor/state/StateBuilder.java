// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.state;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.FunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORCreate;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORJoin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.PreferenceOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.PreferenceOrderType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class StateBuilder {

  // TODO private int currentId = 0;

  private final PredicateTransferRelation ptr;

  /** A copy of the functionCallMap in {@link MPORAlgorithm}. */
  private final ImmutableMap<CFANode, CFANode> functionCallMap;

  /**
   * The set of already existing states, used to prevent the creation of semantically equivalent
   * states.
   */
  private final Set<MPORState> existingStates;

  public StateBuilder(
      PredicateTransferRelation pPtr, ImmutableMap<CFANode, CFANode> pFunctionCallMap) {
    ptr = pPtr;
    functionCallMap = pFunctionCallMap;

    existingStates = new HashSet<>();
  }

  /**
   * Returns the initial MPORState of the program, properly initializing the map from MPORThreads to
   * their start routines / main FunctionEntryNodes, the PreferenceOrders and the corresponding
   * AbstractState.
   *
   * @param pThreads the set of Threads we put in {@link MPORState#threadNodes}
   * @param initAbstractState the initial AbstractState in {@link MPORAlgorithm#run}
   * @return the initial MPORState of the program
   */
  public MPORState createInitState(
      ImmutableSet<MPORThread> pThreads, AbstractState initAbstractState) {

    ImmutableMap.Builder<MPORThread, CFANode> threadNodesBuilder = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      threadNodesBuilder.put(thread, thread.cfa.entryNode);
    }
    ImmutableMap<MPORThread, CFANode> threadNodes = threadNodesBuilder.buildOrThrow();

    ImmutableList.Builder<CFAEdge> emptyTrace = ImmutableList.builder();

    return new MPORState(
        threadNodes,
        initFuncReturnNodes(threadNodes),
        getPreferenceOrdersForThreadNodes(threadNodes),
        new ExecutionTrace(emptyTrace.build()),
        AbstractStates.extractStateByType(initAbstractState, PredicateAbstractState.class));
  }

  /**
   * Returns a new state with the same threadNodes map except that the key pThread is assigned the
   * successor CFANode of pExecutedEdge.
   *
   * @param pState the MPORState from which we execute pExecutedEdge
   * @param pThread The MPORThread that has a new CFANode (= state)
   * @param pExecutedEdge The CFAEdge executed by pThread
   * @return MPORState with CFANode of pThread being the successor of pExecutedEdge
   */
  public MPORState createNewState(
      @NonNull MPORState pState, @NonNull MPORThread pThread, @NonNull CFAEdge pExecutedEdge)
      throws CPATransferException, InterruptedException {

    checkNotNull(pState);
    checkNotNull(pThread);
    checkNotNull(pExecutedEdge);
    checkArgument(pState.threadNodes.containsKey(pThread), "threadNodes must contain pThread");

    // create new threadNodes and executionTrace when executing pExecutedEdge
    ImmutableMap.Builder<MPORThread, CFANode> threadNodesBuilder = ImmutableMap.builder();
    for (var entry : pState.threadNodes.entrySet()) {
      if (!entry.getKey().equals(pThread)) {
        threadNodesBuilder.put(entry);
      }
    }
    threadNodesBuilder.put(pThread, pExecutedEdge.getSuccessor());
    ImmutableMap<MPORThread, CFANode> newThreadNodes = threadNodesBuilder.buildOrThrow();
    ImmutableMap<MPORThread, Optional<CFANode>> newFuncReturnNodes =
        updateFuncReturnNodes(newThreadNodes, pState.funcReturnNodes);
    ExecutionTrace newExecutionTrace = pState.executionTrace.add(pExecutedEdge);

    // to prevent infinite loops, check for a semantically equivalent state
    for (MPORState rExistingState : existingStates) {
      if (areStatesEquivalent(
          rExistingState, newThreadNodes, newFuncReturnNodes, newExecutionTrace)) {
        return rExistingState;
      }
    }
    return new MPORState(
        newThreadNodes,
        newFuncReturnNodes,
        getPreferenceOrdersForThreadNodes(newThreadNodes),
        newExecutionTrace,
        MPORUtil.getNextPredicateAbstractState(ptr, pState.abstractState, pExecutedEdge));
  }

  // FuncReturnNodes ===============================================================================

  /**
   * Creates the initial map of FunctionReturnNodes.
   *
   * @param pInitThreadNodes the map of threads to their main functions / start routines
   *     FunctionEntryNode
   * @return the mapping of MPORThreads to their initial FunctionReturnNodes
   */
  private ImmutableMap<MPORThread, Optional<CFANode>> initFuncReturnNodes(
      ImmutableMap<MPORThread, CFANode> pInitThreadNodes) {
    ImmutableMap.Builder<MPORThread, Optional<CFANode>> rFuncReturnNodes = ImmutableMap.builder();
    for (var entry : pInitThreadNodes.entrySet()) {
      MPORThread currentThread = entry.getKey();
      CFANode currentNode = entry.getValue();
      rFuncReturnNodes.put(currentThread, updateFuncReturnNode(currentNode, Optional.empty()));
    }
    return rFuncReturnNodes.buildOrThrow();
  }

  /**
   * Updates pPrevFuncReturnNodes based on pCurrentThreadNodes.
   *
   * @param pCurrentThreadNodes the current CFANodes for all MPORThreads
   * @param pPrevFuncReturnNodes the current FunctionReturnNodes for all threads to be updated
   */
  public ImmutableMap<MPORThread, Optional<CFANode>> updateFuncReturnNodes(
      ImmutableMap<MPORThread, CFANode> pCurrentThreadNodes,
      ImmutableMap<MPORThread, Optional<CFANode>> pPrevFuncReturnNodes) {

    ImmutableMap.Builder<MPORThread, Optional<CFANode>> rFuncReturnNodes = ImmutableMap.builder();
    for (var entry : pCurrentThreadNodes.entrySet()) {
      MPORThread thread = entry.getKey();
      checkArgument(
          pPrevFuncReturnNodes.containsKey(thread),
          "thread nodes and function return nodes must contain the same threads");
      CFANode currentNode = entry.getValue();
      rFuncReturnNodes.put(
          thread, updateFuncReturnNode(currentNode, pPrevFuncReturnNodes.get(thread)));
    }
    return rFuncReturnNodes.buildOrThrow();
  }

  // Preference Orders =============================================================================

  /**
   * Computes and returns the PreferenceOrders for the current program state pState.
   *
   * @param pThreadNodes the threads and their current CFANodes to be analyzed
   * @return an ImmutableSet of (positional) PreferenceOrders for the given threadNodes
   */
  private ImmutableSet<PreferenceOrder> getPreferenceOrdersForThreadNodes(
      ImmutableMap<MPORThread, CFANode> pThreadNodes) {
    ImmutableSet.Builder<PreferenceOrder> rPreferenceOrders = ImmutableSet.builder();
    for (var entry : pThreadNodes.entrySet()) {
      MPORThread currentThread = entry.getKey();
      CFANode currentNode = entry.getValue();
      rPreferenceOrders.addAll(getCreatePreferenceOrder(pThreadNodes, currentThread, currentNode));
      rPreferenceOrders.addAll(getMutexPreferenceOrders(pThreadNodes, currentThread, currentNode));
      rPreferenceOrders.addAll(getJoinPreferenceOrders(pThreadNodes, currentThread, currentNode));
      // TODO getBarrierPreferenceOrders
    }
    return rPreferenceOrders.build();
  }

  /**
   * @param pThreadNodes the threads and their current CFANodes
   * @param pCreatingThread the thread where we check if it is calling pthread_create
   * @param pCreatingNode the current CFANode of pCreatingThread
   * @return the set of PreferenceOrders induced by pthread_create calls
   */
  private ImmutableSet<PreferenceOrder> getCreatePreferenceOrder(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      MPORThread pCreatingThread,
      CFANode pCreatingNode) {

    ImmutableSet.Builder<PreferenceOrder> rCreatePreferenceOrders = ImmutableSet.builder();

    // if any thread is at their entryNode
    for (var entry : pThreadNodes.entrySet()) {
      MPORThread createdThread = entry.getKey();
      if (!createdThread.equals(pCreatingThread)) {
        if (entry.getValue().equals(createdThread.cfa.entryNode)) {
          // check if pCreatingThread creates the thread which is at its entry
          for (MPORCreate create : pCreatingThread.cfa.creates) {
            if (create.createdPthreadT.equals(createdThread.threadObject.orElseThrow())) {
              ImmutableSet<CFAEdge> subsequentEdges =
                  ImmutableSet.copyOf(CFAUtils.leavingEdges(createdThread.cfa.entryNode));
              rCreatePreferenceOrders.add(
                  new PreferenceOrder(
                      PreferenceOrderType.CREATE,
                      pCreatingThread,
                      createdThread,
                      create.precedingEdges,
                      subsequentEdges));
            }
          }
        }
      }
    }
    return rCreatePreferenceOrders.build();
  }

  /**
   * Computes and returns the PreferenceOrders induced by mutex locks in the program.
   *
   * @param pThreadNodes the threads and their current CFANodes
   * @param pThreadInMutex the thread where we check if it is inside a mutex lock
   * @param pNodeInMutex the current CFANode of pThreadInMutex
   * @return the set of PreferenceOrders induced by mutex locks
   */
  private ImmutableSet<PreferenceOrder> getMutexPreferenceOrders(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      MPORThread pThreadInMutex,
      CFANode pNodeInMutex) {

    ImmutableSet.Builder<PreferenceOrder> rMutexPreferenceOrders = ImmutableSet.builder();

    // if pThreadInMutex is in a mutex lock
    for (MPORMutex mutex : pThreadInMutex.cfa.mutexes) {
      if (mutex.nodes.contains(pNodeInMutex)) {

        // search all other threads for pthread_mutex_lock calls to the same pthread_mutex_t object
        for (var entry : pThreadNodes.entrySet()) {
          MPORThread lockingThread = entry.getKey();
          if (!lockingThread.equals(pThreadInMutex)) {
            CFANode otherNode = entry.getValue();
            for (CFAEdge cfaEdge : CFAUtils.leavingEdges(otherNode)) {
              if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_MUTEX_LOCK)) {
                CExpression pthreadMutexT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
                if (pthreadMutexT.equals(mutex.pthreadMutexT)) {

                  // extract all CFAEdges inside mutex excluding the leaving edges of exitNodes
                  ImmutableSet.Builder<CFAEdge> precedingEdges = ImmutableSet.builder();
                  precedingEdges.addAll(mutex.edges);
                  rMutexPreferenceOrders.add(
                      new PreferenceOrder(
                          PreferenceOrderType.MUTEX,
                          pThreadInMutex,
                          lockingThread,
                          precedingEdges.build(),
                          ImmutableSet.copyOf(CFAUtils.leavingEdges(otherNode))));
                }
              }
            }
          }
        }
      }
    }
    return rMutexPreferenceOrders.build();
  }

  /**
   * Computes and returns the PreferenceOrders induced by joins in the program.
   *
   * @param pThreadNodes the map of all threads to their current CFANode in the program
   * @param pJoiningThread the thread where we check if it is calling pthread_join
   * @param pJoiningNode the current CFANode of pJoiningThread
   * @return the set of PreferenceOrders induced by joins
   */
  private ImmutableSet<PreferenceOrder> getJoinPreferenceOrders(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      MPORThread pJoiningThread,
      CFANode pJoiningNode) {

    ImmutableSet.Builder<PreferenceOrder> rJoinPreferenceOrders = ImmutableSet.builder();
    // if pJoiningThread is right before a pthread_join call
    for (MPORJoin join : pJoiningThread.cfa.joins) {
      if (pJoiningNode.equals(join.preJoinNode)) {
        CExpression pthreadT = CFAUtils.getParameterAtIndex(join.joinEdge, 0);
        MPORThread targetThread = getThreadByPthreadT(pThreadNodes, pthreadT);
        // if the thread specified as pthread_t in the pthread_join call has not yet terminated
        CFANode targetThreadNode = pThreadNodes.get(targetThread);
        assert targetThreadNode != null;
        if (!targetThreadNode.equals(targetThread.cfa.exitNode)) {
          // TODO use predecessor / successor of edges for pcs in seq
          // add all CFAEdges executed by pthread_t as preceding edges
          ImmutableSet<CFAEdge> precedingEdges = null;
          ImmutableSet<CFAEdge> subsequentEdges =
              ImmutableSet.copyOf(CFAUtils.leavingEdges(join.preJoinNode));
          rJoinPreferenceOrders.add(
              new PreferenceOrder(
                  PreferenceOrderType.JOIN,
                  targetThread,
                  pJoiningThread,
                  precedingEdges,
                  subsequentEdges));
        }
      }
    }
    return rJoinPreferenceOrders.build();
  }

  // (Private) Helpers =============================================================================

  /**
   * Checks whether pStateAs threadNodes and the tail of executionTrace equal pThreadNodesB and the
   * tail of pExecutionTraceB, respectively. Note that the tail only approximates equivalence and
   * does not guarantee it.
   *
   * @return true if pStates threadNodes and executionTrace equal pThreadNodesB and pExecutionTraceB
   */
  private boolean areStatesEquivalent(
      MPORState pStateA,
      ImmutableMap<MPORThread, CFANode> pThreadNodesB,
      ImmutableMap<MPORThread, Optional<CFANode>> pFuncReturnNodesB,
      ExecutionTrace pExecutionTraceB) {

    // TODO we should optimize this function by creating orders for the conditions
    //  e.g. if we have 2 threads but the tail is 3 elements, we should check the threads and
    //  funcreturnnodes first. if it is the other way around, checking the tail first is more
    //  performant
    return pStateA.threadNodes.equals(pThreadNodesB)
        && pStateA.funcReturnNodes.equals(pFuncReturnNodesB)
        && pStateA.executionTrace.tail().equals(pExecutionTraceB.tail());
  }

  /**
   * Searches the given map of MPORThreads for the given pPthreadT object.
   *
   * @param pThreadNodes the map of MPORThreads to their current CFANodes to be searched
   * @param pPthreadT the pthread_t object as a CExpression
   * @return the MPORThread object with pPthreadT as its threadObject (pthread_t)
   * @throws IllegalArgumentException if no thread exists in the map whose threadObject is pPthreadT
   */
  private MPORThread getThreadByPthreadT(
      ImmutableMap<MPORThread, CFANode> pThreadNodes, CExpression pPthreadT) {
    for (var entry : pThreadNodes.entrySet()) {
      MPORThread rThread = entry.getKey();
      if (rThread.threadObject.isPresent()) {
        if (rThread.threadObject.orElseThrow().equals(pPthreadT)) {
          return rThread;
        }
      }
    }
    throw new IllegalArgumentException("no MPORThread with pPthreadT found in pThreads");
  }

  private Optional<CFANode> updateFuncReturnNode(
      CFANode pCurrentNode, Optional<CFANode> pPrevFuncReturnNode) {
    return MPORUtil.updateFuncReturnNode(functionCallMap, pCurrentNode, pPrevFuncReturnNode);
  }

  // Getters =======================================================================================

  // TODO optimize this by using another data structure instead of set
  public Set<MPORState> getExistingStates() {
    return existingStates;
  }
}
