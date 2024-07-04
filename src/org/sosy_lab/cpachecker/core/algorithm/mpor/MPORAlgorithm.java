// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORJoin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORMutex;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This is an implementation of a Partial Order Reduction (POR) algorithm, presented in the 2022
 * paper "Sound Sequentialization for Concurrent Program Verification". This algorithm aims at
 * producing a reduced sequentialization of a parallel C program. The reduced sequentialization can
 * be given to an existing verifier capable of verifying sequential C programs. The POR and the
 * verifier serve as modules, hence MPOR (Modular Partial Order Reduction).
 *
 * <p>Using an unbounded number of threads (e.g. while(true) { pthread_create... }) is undefined
 * behavior.
 */
@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  // TODO remove all @SuppressWarnings once finished

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // TODO this method is called once initially with the set of reached states in the ARG
    throw new UnsupportedOperationException("Unimplemented method 'run'");
  }

  private final ConfigurableProgramAnalysis cpa;

  private final LogManager logger;

  private final Configuration config;

  private final ShutdownNotifier shutdownNotifier;

  private final Specification specification;

  private final CFA cfa;

  /** A map of functions to sets of functions that are called inside of them. */
  private Map<CFunctionType, Set<CFunctionType>> functionCallHierarchy;

  /** A set of functions that are start routines extracted from pthread_create calls. */
  private Set<CFunctionType> threadStartRoutines;

  /** A map of thread IDs to functions executed by the thread. */
  private Map<Integer, Set<CFunctionType>> threadIdFunctions;

  /** A map of thread IDs to CFANodes the threads are currently in. */
  private Map<Integer, CFANode> threadNodes;

  /** The set of pthread_t objects in the program, i.e. threads */
  private Set<MPORThread> threads;

  /** A map from FunctionCallEdge Predecessors to Return Nodes. */
  private Map<CFANode, CFANode> functionCallMap;

  // TODO a reduced and sequentialized CFA that is created based on the POR algorithm

  public MPORAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa) {

    cpa = pCpa;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;
    cfa = pCfa;

    // TODO group methods into prechecks, object extractions, actual algorithms on the CFA, etc.
    checkForCProgram(cfa);
    checkForParallelProgram(cfa);
    functionCallMap = getFunctionCallMap(cfa);
    // TODO performance stuff:
    //  merge functions that go through each Edge together into one
    //  merge functions that go through each Node together into one
    threads = getThreads(cfa);
    assignMutexesToThreads(threads);
    assignJoinsToThreads(threads);
    // TODO assignBarriersToThreads(threads);
  }

  /** Checks whether the input language of the program is C and throws an exception if not. */
  private void checkForCProgram(CFA pCfa) {
    Preconditions.checkArgument(
        pCfa.getMetadata().getInputLanguage().equals(Language.C), "MPOR expects C program");
  }

  /**
   * Checks whether any edge in the CFA contains a pthread_create call. If that is not the case, the
   * algorithm ends and the user is informed that MPOR is meant to analyze parallel programs.
   */
  private void checkForParallelProgram(CFA pCfa) {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.CREATE)) {
        isParallel = true;
        break;
      }
    }
    Preconditions.checkArgument(
        isParallel, "MPOR expects parallel C program with at least one pthread_create call");
  }

  /**
   * Searches all CFAEdges in pCfa for FunctionCallEdges and maps the predecessor CFANodes to their
   * ReturnNodes so that context-sensitive algorithms can be performed on the CFA.
   *
   * <p>E.g. a FunctionExitNode may have several leaving Edges, one for each time the function is
   * called. With the Map, extracting only the leaving Edge resulting in the ReturnNode is possible.
   *
   * @param pCfa the CFA to be analyzed
   * @return A Map of CFANodes before a FunctionCallEdge (keys) to the CFANodes where a function
   *     continues (values, i.e. the ReturnNode) after going through the CFA of the function called.
   */
  private Map<CFANode, CFANode> getFunctionCallMap(CFA pCfa) {
    Map<CFANode, CFANode> rFunctionCallMap = new HashMap<>();
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (cfaEdge instanceof FunctionCallEdge functionCallEdge) {
        rFunctionCallMap.put(functionCallEdge.getPredecessor(), functionCallEdge.getReturnNode());
      }
    }
    return rFunctionCallMap;
  }

  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * @param pCfa the CFA to be analyzed
   * @return the set of threads
   */
  private Set<MPORThread> getThreads(CFA pCfa) {
    Set<MPORThread> rThreads = new HashSet<>();

    // add the main thread
    CFunctionType mainFunction = CFAUtils.getMainFunction(pCfa);
    FunctionEntryNode mainEntryNode =
        CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, mainFunction);
    Optional<FunctionExitNode> mainExitNode = mainEntryNode.getExitNode();
    MPORThread mainThread = new MPORThread(Optional.empty(), mainEntryNode, mainExitNode);
    rThreads.add(mainThread);

    // search the CFA for pthread_create calls
    for (CFAEdge cfaEdge : CFAUtils.allUniqueEdges(pCfa)) {
      // TODO use loop structure to handle pthread_create calls inside loops
      //  (function is called numerous times)
      //  note that in loops, the CExpression for pthreadT is not unique but always the same
      if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.CREATE)) {
        // extract the first parameter of pthread_create, i.e. the pthread_t value
        CExpression pthreadT =
            CFAUtils.getValueFromPointer(CFAUtils.getParameterAtIndex(cfaEdge, 0));
        // extract the third parameter of pthread_create which points to the start routine function
        CFunctionType startRoutine =
            CFAUtils.getCFunctionTypeFromCExpression(CFAUtils.getParameterAtIndex(cfaEdge, 2));
        FunctionEntryNode entryNode =
            CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, startRoutine);
        Optional<FunctionExitNode> exitNode = entryNode.getExitNode();
        MPORThread pthread = new MPORThread(Optional.ofNullable(pthreadT), entryNode, exitNode);
        rThreads.add(pthread);
      }
    }
    return rThreads;
  }

  /**
   * Searches all threads in pThreads for mutex locks and properly initializes the corresponding
   * MPORMutex objects.
   *
   * @param pThreads the set of threads whose main functions / start routines are searched
   */
  private void assignMutexesToThreads(Set<MPORThread> pThreads) {
    for (MPORThread thread : pThreads) {
      Set<CFANode> visitedNodes = new HashSet<>();
      searchThreadForMutexes(thread, visitedNodes, thread.entryNode, null);
    }
  }

  /**
   * Recursively searches the CFA of pThread for mutex_locks.
   *
   * @param pThread the thread to be searched
   * @param pVisitedNodes keep track of already visited CFANodes to prevent an infinite loop if
   *     there are loop structures in the CFA
   * @param pCurrentNode the CFANode whose leaving CFAEdges we analyze for mutex_locks
   * @param pFunctionReturnNode used to track the original context when entering the CFA of another
   *     function. see {@link MPORAlgorithm#getFunctionCallMap(CFA)} for more info.
   */
  private void searchThreadForMutexes(
      MPORThread pThread,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    if (pThread.exitNode.isPresent()) {
      if (pVisitedNodes.contains(pCurrentNode)
          || pCurrentNode.equals(pThread.exitNode.orElseThrow())) {
        return;
      }
      for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
        if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.MUTEX_LOCK)) {
          CExpression pthreadMutexT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
          // the successor node of mutex_lock is the first inside the lock
          MPORMutex mutex = new MPORMutex(pthreadMutexT, cfaEdge.getSuccessor());
          findMutexCfaNodes(pThread, mutex, cfaEdge.getSuccessor(), null);
        }
        pVisitedNodes.add(pCurrentNode);
        searchThreadForMutexes(
            pThread,
            pVisitedNodes,
            cfaEdge.getSuccessor(),
            getFunctionReturnNode(pCurrentNode, pFunctionReturnNode, functionCallMap));
      }
    } else {
      // TODO logic if there is no FunctionExitNode for the pThread
      //  undefined behavior / restriction?
    }
  }

  /**
   * Recursively searches the CFA of pThread for all CFANodes inside pMutex, i.e. until one or more
   * mutex_unlock is encountered.
   *
   * @param pThread the thread whose CFA we analyze
   * @param pMutex the mutex lock whose CFANodes we want to find
   * @param pCurrentNode the current CFANode whose leaving Edges we search for mutex_unlocks
   * @param pFunctionReturnNode used to track the original context when entering the CFA of another
   *     function. see {@link MPORAlgorithm#getFunctionCallMap(CFA)} for more info.
   */
  private void findMutexCfaNodes(
      MPORThread pThread, MPORMutex pMutex, CFANode pCurrentNode, CFANode pFunctionReturnNode) {

    // visit CFANodes only once to prevent infinite loops in case of loop structures
    if (!pMutex.getNodes().contains(pCurrentNode)) {
      pMutex.getNodes().add(pCurrentNode);
      for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
        if (PthreadFunctionType.isEdgeCallToFunctionType(
            cfaEdge, PthreadFunctionType.MUTEX_UNLOCK)) {
          CExpression pthreadMutexT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
          if (pthreadMutexT.equals(pMutex.pthreadMutexT)) {
            // the last node inside the lock, before unlocking
            pMutex.addExitNode(pCurrentNode);
            // here, the mutex might be in the set already if there are multiple paths to an unlock
            pThread.addMutex(pMutex);
          }
        } else {
          findMutexCfaNodes(
              pThread,
              pMutex,
              cfaEdge.getSuccessor(),
              getFunctionReturnNode(pCurrentNode, pFunctionReturnNode, functionCallMap));
        }
      }
    }
  }

  /**
   * Searches all threads in pThreads for pthread_join calls and properly initializes the
   * corresponding MPORJoin objects.
   *
   * @param pThreads the set of threads whose main functions / start routines are searched
   */
  private void assignJoinsToThreads(Set<MPORThread> pThreads) {
    for (MPORThread thread : pThreads) {
      Set<CFANode> visitedNodes = new HashSet<>();
      searchThreadForJoins(pThreads, thread, visitedNodes, thread.entryNode, null);
    }
  }

  /**
   * Recursively searches the CFA of pThread for pthread_join calls.
   *
   * @param pThreads the entire set of threads, used to reference the thread that is waited on in
   *     the join call
   * @param pThread the thread to be searched
   * @param pVisitedNodes keep track of already visited CFANodes to prevent an infinite loop if
   *     there are loop structures in the CFA
   * @param pCurrentNode the CFANode whose leaving CFAEdges we analyze for pthread_joins
   * @param pFunctionReturnNode used to track the original context when entering the CFA of another
   *     function. see {@link MPORAlgorithm#getFunctionCallMap(CFA)} for more info.
   */
  private void searchThreadForJoins(
      Set<MPORThread> pThreads,
      MPORThread pThread,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    if (pThread.exitNode.isPresent()) {
      if (pVisitedNodes.contains(pCurrentNode)
          || pCurrentNode.equals(pThread.exitNode.orElseThrow())) {
        return;
      }
      for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
        if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.JOIN)) {
          CExpression pthreadT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
          MPORThread threadToTerminate = getThreadByPthreadT(pThreads, pthreadT);
          MPORJoin join = new MPORJoin(threadToTerminate, pCurrentNode);
          pThread.addJoin(join);
        }
        pVisitedNodes.add(pCurrentNode);
        searchThreadForJoins(
            pThreads,
            pThread,
            pVisitedNodes,
            cfaEdge.getSuccessor(),
            getFunctionReturnNode(pCurrentNode, pFunctionReturnNode, functionCallMap));
      }
    } else {
      // TODO
    }
  }

  /**
   * TODO
   *
   * @param pThreads TODO
   */
  private void assignBarriersToThreads(Set<MPORThread> pThreads) {
    for (MPORThread thread : pThreads) {
      Set<CFANode> visitedNodes = new HashSet<>();
      searchThreadForBarriers(thread, visitedNodes, thread.entryNode, null);
    }
  }

  /**
   * TODO
   *
   * @param pThread TODO
   * @param pVisitedNodes TODO
   * @param pCurrentNode TODO
   * @param pFunctionReturnNode TODO
   */
  private void searchThreadForBarriers(
      MPORThread pThread,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {
    // TODO see pthread-divine for example barrier programs
    if (pThread.exitNode.isPresent()) {
      if (pVisitedNodes.contains(pCurrentNode)
          || pCurrentNode.equals(pThread.exitNode.orElseThrow())) {
        return;
      }
      for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
        if (PthreadFunctionType.isEdgeCallToFunctionType(
            cfaEdge, PthreadFunctionType.BARRIER_INIT)) {
          // TODO unsure how to handle this, SV benchmarks use their custom barrier objects and
          //  functions, e.g. pthread-divine/barrier_2t.i
          //  but the general approach is identifying MPORBarriers from barrier_init calls
          //  and then identify the corresponding MPORBarrierWaits
        }
        pVisitedNodes.add(pCurrentNode);
        searchThreadForBarriers(
            pThread,
            pVisitedNodes,
            cfaEdge.getSuccessor(),
            getFunctionReturnNode(pCurrentNode, pFunctionReturnNode, functionCallMap));
      }
    } else {
      // TODO
    }
  }

  // TODO positional preference order ("a <q b") possible cases:
  //  pthread_mutex_lock / mutex_unlock
  //  pthread_join
  //  pthread_barrier_wait
  //  pthread_mutex_cond_wait / cond_signal
  //  pthread_rwlock_rdlock / unlock
  //  pthread_rwlock_wrlock / unlock
  //  pthread_key_create / setspecific
  //  flags (e.g. while (flag == 0); though this is difficult to extract from the code?)
  //  __atomic_store_n / __atomic_load_n
  //  atomic blocks
  //  sequential blocks

  // TODO use GlobalAccessChecker to check whether a CfaEdge reads or writes global / shared
  //  variables?

  // TODO use CFAToCTranslator translateCfa to generate a C program based on a CFA
  //  this will be used for the reduced and sequentialized CFA

  // Helpers =======================================================================================

  /**
   * Searches the given Set of MPORThreads for the given pPthreadT object.
   *
   * @param pThreads the set of MPORThreads to be searched
   * @param pPthreadT the pthread_t object as a CExpression
   * @return the MPORThread object with pPthreadT as its threadObject (pthread_t)
   * @throws IllegalArgumentException if no thread exists in the set whose threadObject is pPthreadT
   */
  public static MPORThread getThreadByPthreadT(Set<MPORThread> pThreads, CExpression pPthreadT) {
    for (MPORThread rThread : pThreads) {
      if (rThread.threadObject.isPresent()) {
        if (rThread.threadObject.orElseThrow().equals(pPthreadT)) {
          return rThread;
        }
      }
    }
    throw new IllegalArgumentException("no MPORThread with pPthreadT found in pThreads");
  }

  /**
   * Searches pFunctionCallMap for pCurrentNode. If the key is present, the FunctionReturnNode is
   * returned. If not, we take the previous pFunctionReturnNode or reset it to null if pCurrentNode
   * is a FunctionExitNode, i.e. the previous pFunctionReturnNode is not relevant anymore in the
   * next iteration.
   *
   * @param pCurrentNode the current CFANode to be analyzed
   * @param pFunctionReturnNode the previous pFunctionReturnNode
   * @param pFunctionCallMap map from CFANodes before FunctionCallEdges to FunctionReturnNodes
   * @return the previous or new FunctionReturnNode or null if pCurrentNode exits a function
   */
  public static CFANode getFunctionReturnNode(
      CFANode pCurrentNode, CFANode pFunctionReturnNode, Map<CFANode, CFANode> pFunctionCallMap) {
    return pFunctionCallMap.getOrDefault(
        pCurrentNode,
        // reset the FunctionReturnNode when encountering a FunctionExitNode
        pCurrentNode instanceof FunctionExitNode ? null : pFunctionReturnNode);
  }

  /**
   * If pCurrentNode is a FunctionExitNode, i.e. it's successor CFANodes are all nodes where the
   * function is called, we return only the CFAEdge whose successor is pFunctionReturnNode (the
   * original calling context).
   *
   * @param pCurrentNode the CFANode whose leaving Edges we analyze
   * @param pFunctionReturnNode the return node (extracted from the original functionCallEdge)
   * @return a FluentIterable of context-sensitive leaving CFAEdges of pCurrentNode
   */
  public static FluentIterable<CFAEdge> contextSensitiveLeavingEdges(
      CFANode pCurrentNode, CFANode pFunctionReturnNode) {
    // if pCurrentNode is a FunctionExitNode, consider only the edge leading to pFunctionReturnNode
    if (pCurrentNode instanceof FunctionExitNode) {
      return CFAUtils.leavingEdges(pCurrentNode)
          .filter(
              cfaEdge ->
                  !(cfaEdge instanceof FunctionSummaryEdge) // exclude parallel edges
                      && cfaEdge.getSuccessor().equals(pFunctionReturnNode));
    } else {
      return CFAUtils.leavingEdges(pCurrentNode);
    }
  }
}
