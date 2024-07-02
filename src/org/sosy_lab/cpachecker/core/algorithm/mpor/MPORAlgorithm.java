// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.common.base.Preconditions;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
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

  /** The set of pthread_mutex_t objects in the program. */
  private Set<CIdExpression> mutexObjects;

  /** The set of pthread_t objects in the program, i.e. threads */
  private Set<MPORThread> threads;

  /** A map from FunctionCallEdge Predecessors to Return Nodes. */
  private Map<CFANode, CFANode> functionCallEdgeNodes;

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
    functionCallEdgeNodes = getFunctionCallEdgeNodes(cfa);
    // TODO performance stuff:
    //  merge functions that go through each Edge together into one
    //  merge functions that go through each Node together into one
    threads = getThreads(cfa);
    assignMutexesToThreads(threads);
    assignJoinsToThreads(threads);
    // TODO
    // assignBarriersToThreads(threads);
    mutexObjects = getMutexes(cfa);

    // TODO create MPORState class mapping MPORThreads to their current location (CFANode)
    // TODO not sure if we will actually use these, keeping them for now
    // functionCallHierarchy = getFunctionCallHierarchy(cfa);
    // threadStartRoutines = getThreadStartRoutines(cfa);
    // threadIdFunctions = getFunctionThreadIds(threadStartRoutines, functionCallHierarchy);
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
  private Map<CFANode, CFANode> getFunctionCallEdgeNodes(CFA pCfa) {
    Map<CFANode, CFANode> ret = new HashMap<>();
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (cfaEdge instanceof FunctionCallEdge functionCallEdge) {
        ret.put(functionCallEdge.getPredecessor(), functionCallEdge.getReturnNode());
      }
    }
    return ret;
  }

  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * @param pCfa the CFA to be analyzed
   * @return the set of threads
   */
  private Set<MPORThread> getThreads(CFA pCfa) {
    Set<MPORThread> ret = new HashSet<>();

    // add the main thread
    CFunctionType mainFunction = CFAUtils.getMainFunction(pCfa);
    FunctionEntryNode mainEntryNode =
        CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, mainFunction);
    Optional<FunctionExitNode> mainExitNode = mainEntryNode.getExitNode();
    MPORThread mainThread = new MPORThread(Optional.empty(), mainEntryNode, mainExitNode);
    ret.add(mainThread);

    // search the CFA for pthread_create calls
    for (CFAEdge cfaEdge : CFAUtils.allUniqueEdges(pCfa)) {
      // TODO use loop structure to handle pthread_create calls inside loops
      //  (function is called numerous times)
      if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.CREATE)) {
        // TODO find out what happens if we access an array of pthread_t objects
        //  what will the CIdExpression be, the pthread_t object or the array?
        //  see pthread-divine/barrier_2t.i, pthread_create call is inside loop and using an array
        // extract the first parameter of pthread_create, i.e. the pthread_t object
        CUnaryExpression pthreadTExpression =
            (CUnaryExpression) CFAUtils.getParameterAtIndex(cfaEdge, 0);
        Optional<CIdExpression> pthreadT =
            Optional.ofNullable((CIdExpression) pthreadTExpression.getOperand());

        // extract the third parameter of pthread_create which points to the start routine function
        CUnaryExpression startRoutineExpression =
            (CUnaryExpression) CFAUtils.getParameterAtIndex(cfaEdge, 2);
        CPointerType cPointerType = (CPointerType) startRoutineExpression.getExpressionType();
        CFunctionType startRoutine = (CFunctionType) cPointerType.getType();

        FunctionEntryNode entryNode =
            CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, startRoutine);
        Optional<FunctionExitNode> exitNode = entryNode.getExitNode();

        MPORThread pthread = new MPORThread(pthreadT, entryNode, exitNode);
        ret.add(pthread);
      }
    }
    return ret;
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
   *     function. see {@link MPORAlgorithm#getFunctionCallEdgeNodes(CFA)} for more info.
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
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pCurrentNode)) {
        // TODO this is the exact same procedure as in findMutexCfaNodes, create a function?
        // if pCurrentNode is a FunctionExitNode, only consider the original calling context
        if (pCurrentNode instanceof FunctionExitNode) {
          if (!cfaEdge.getSuccessor().equals(pFunctionReturnNode)) {
            continue;
          }
          pFunctionReturnNode = null; // no need, but useful for debugging and cleaner
        }
        if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.MUTEX_LOCK)) {
          CIdExpression pthreadMutexT = (CIdExpression) CFAUtils.getParameterAtIndex(cfaEdge, 0);
          // the successor node of mutex_lock is the first inside the lock
          MPORMutex mutex = new MPORMutex(pthreadMutexT, cfaEdge.getSuccessor());
          findMutexCfaNodes(pThread, mutex, cfaEdge.getSuccessor(), null);
        }
        pVisitedNodes.add(pCurrentNode);
        searchThreadForMutexes(
            pThread,
            pVisitedNodes,
            cfaEdge.getSuccessor(),
            functionCallEdgeNodes.getOrDefault(pCurrentNode, pFunctionReturnNode));
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
   *     function. see {@link MPORAlgorithm#getFunctionCallEdgeNodes(CFA)} for more info.
   */
  private void findMutexCfaNodes(
      MPORThread pThread, MPORMutex pMutex, CFANode pCurrentNode, CFANode pFunctionReturnNode) {

    // visit CFANodes only once to prevent infinite loops in case of loop structures
    if (!pMutex.getNodes().contains(pCurrentNode)) {
      pMutex.getNodes().add(pCurrentNode);
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pCurrentNode)) {

        // if pCurrentNode is a FunctionExitNode, only consider the original calling context
        if (pCurrentNode instanceof FunctionExitNode) {
          if (!cfaEdge.getSuccessor().equals(pFunctionReturnNode)) {
            continue;
          }
          pFunctionReturnNode = null; // no need, but useful for debugging and cleaner
        }

        if (PthreadFunctionType.isEdgeCallToFunctionType(
            cfaEdge, PthreadFunctionType.MUTEX_UNLOCK)) {
          CIdExpression pthreadMutexT = (CIdExpression) CFAUtils.getParameterAtIndex(cfaEdge, 0);
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
              functionCallEdgeNodes.getOrDefault(pCurrentNode, pFunctionReturnNode));
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
   *     function. see {@link MPORAlgorithm#getFunctionCallEdgeNodes(CFA)} for more info.
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
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pCurrentNode)) {
        // if pCurrentNode is a FunctionExitNode, only consider the original calling context
        if (pCurrentNode instanceof FunctionExitNode) {
          if (!cfaEdge.getSuccessor().equals(pFunctionReturnNode)) {
            continue;
          }
          pFunctionReturnNode = null; // no need, but useful for debugging and cleaner
        }
        if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.JOIN)) {
          CExpression cExpression = CFAUtils.getParameterAtIndex(cfaEdge, 0);
          CIdExpression pthreadT = (CIdExpression) CFAUtils.getParameterAtIndex(cfaEdge, 0);
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
            functionCallEdgeNodes.getOrDefault(pCurrentNode, pFunctionReturnNode));
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
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pCurrentNode)) {
        // if pCurrentNode is a FunctionExitNode, only consider the original calling context
        if (pCurrentNode instanceof FunctionExitNode) {
          if (!cfaEdge.getSuccessor().equals(pFunctionReturnNode)) {
            continue;
          }
          pFunctionReturnNode = null; // no need, but useful for debugging and cleaner
        }
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
            functionCallEdgeNodes.getOrDefault(pCurrentNode, pFunctionReturnNode));
      }
    } else {
      // TODO
    }
  }

  /**
   * Goes through the given CFA and extracts all functions and functions called inside of them. The
   * map also contains functions that do not call other functions inside of them.
   *
   * @param pCfa the CFA to be analyzed
   * @return a map of functions (keys) to a set functions called inside of them (values)
   */
  private Map<CFunctionType, Set<CFunctionType>> getFunctionCallHierarchy(CFA pCfa) {
    Map<CFunctionType, Set<CFunctionType>> callHierarchy = new HashMap<>();
    for (CFANode cfaNode : pCfa.nodes()) {
      CFunctionType caller = (CFunctionType) cfaNode.getFunction().getType();
      if (!callHierarchy.containsKey(caller)) {
        // add the function as a key, no matter if it actually calls other functions inside of it
        callHierarchy.put(caller, new HashSet<>());
      }
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(cfaNode)) {
        CFANode successor = cfaEdge.getSuccessor();
        if (successor instanceof FunctionEntryNode) {
          callHierarchy.get(caller).add((CFunctionType) successor.getFunction().getType());
        }
      }
    }
    return callHierarchy;
  }

  /**
   * Searches the CFA for phtread_create calls and returns the start routines (i.e. the function the
   * thread is executing).
   *
   * @param pCfa the CFA to be analyzed
   * @return set of functions that are start routines in pthread_create calls
   */
  private Set<CFunctionType> getThreadStartRoutines(CFA pCfa) {
    Set<CFunctionType> startRoutines = new HashSet<>();
    // add the main function as the first element of the set
    startRoutines.add(CFAUtils.getMainFunction(pCfa));
    // search the CFA for pthread_create calls
    for (CFAEdge cfaEdge : CFAUtils.allUniqueEdges(pCfa)) {
      if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.CREATE)) {
        // TODO is there a way to shorten all these casts ... ?
        // TODO use loop structure to handle pthread_create calls inside loops
        //  (function is called numerous times)
        // extract the third parameter of pthread_create which points to the start routine function
        CUnaryExpression cUnaryExpression =
            (CUnaryExpression) CFAUtils.getParameterAtIndex(cfaEdge, 2);
        CPointerType cPointerType = (CPointerType) cUnaryExpression.getExpressionType();
        CFunctionType cFunctionType = (CFunctionType) cPointerType.getType();
        startRoutines.add(cFunctionType);
      }
    }
    return startRoutines;
  }

  /**
   * @param pThreadStartRoutines the set of functions serving as start routines for threads
   * @param pFunctionCallHierarchy the mapping from functions to functions called inside of them
   * @return the mapping from functions to a set of thread IDs executing them
   */
  private Map<Integer, Set<CFunctionType>> getFunctionThreadIds(
      Set<CFunctionType> pThreadStartRoutines,
      Map<CFunctionType, Set<CFunctionType>> pFunctionCallHierarchy) {

    Map<Integer, Set<CFunctionType>> functionThreadIdMap = new HashMap<>();
    // note that the threadIds in this analysis may not be the same as when actually executing the
    // program. the main thread may not necessarily be the one with threadId 0.
    int currentThreadId = 0;
    // go through all thread start routines and recursively check for function calls inside of them
    for (CFunctionType startRoutine : pThreadStartRoutines) {
      Set<CFunctionType> visitedCFunctionTypes = new HashSet<>();
      exploreFunctionCalls(
          pFunctionCallHierarchy,
          functionThreadIdMap,
          startRoutine,
          visitedCFunctionTypes,
          currentThreadId);
      currentThreadId++;
    }
    return functionThreadIdMap;
  }

  /**
   * Searches the CFA for pthread_mutex_t objects given as parameters to pthread_mutex_lock calls.
   *
   * @param pCfa the CFA to be analyzed
   * @return set of CIdExpressions that are instances of pthread_mutex_t used in pthread_mutex_locks
   */
  // TODO what about pthread_mutex_trylock?
  // TODO unsure if we should consider MUTEX_INIT too?
  private Set<CIdExpression> getMutexes(CFA pCfa) {
    Set<CIdExpression> mutexes = new HashSet<>();
    for (CFAEdge cfaEdge : pCfa.edges()) {
      if (PthreadFunctionType.isEdgeCallToFunctionType(cfaEdge, PthreadFunctionType.MUTEX_LOCK)) {
        // extract the first parameter which is the pthread_mutex_t object
        CUnaryExpression cUnaryExpression =
            (CUnaryExpression) CFAUtils.getParameterAtIndex(cfaEdge, 0);
        CIdExpression cIdExpression = (CIdExpression) cUnaryExpression.getOperand();
        mutexes.add(cIdExpression);
      }
    }
    return mutexes;
  }

  /**
   * Recursively iterates over the functions called in pCFunctionType and assigns pThreadId to them
   * in pFunctionThreadIdMap.
   *
   * @param pFunctionCallHierarchy the mapping of functions to a set of functions called inside them
   * @param pFunctionThreadIdMap the mapping of functions to a set of thread IDs executing it
   * @param pCFunctionType the pCFunctionType whose function calls we extract
   * @param pVisitedCFunctionTypes the set of CFunctionTypes that were pCFunctionType once to
   *     prevent an infinite loop
   * @param pThreadId the ID of the thread we assign the functions to in pFunctionThreadIdMap
   * @return the mapping of functions to thread IDs executing them
   */
  private Map<Integer, Set<CFunctionType>> exploreFunctionCalls(
      Map<CFunctionType, Set<CFunctionType>> pFunctionCallHierarchy,
      Map<Integer, Set<CFunctionType>> pFunctionThreadIdMap,
      CFunctionType pCFunctionType,
      Set<CFunctionType> pVisitedCFunctionTypes,
      int pThreadId) {

    // add function to already visited functions in case there is recursion in the original program
    if (!pVisitedCFunctionTypes.contains(pCFunctionType)) {
      pVisitedCFunctionTypes.add(pCFunctionType);
      // not checking if the key exists here because this should always be the case
      Set<CFunctionType> calledFunctions = pFunctionCallHierarchy.get(pCFunctionType);
      for (CFunctionType newCFunctionType : calledFunctions) {
        if (!pFunctionThreadIdMap.containsKey(pThreadId)) {
          pFunctionThreadIdMap.put(pThreadId, new HashSet<>());
        }
        pFunctionThreadIdMap.get(pThreadId).add(newCFunctionType);
        // recursively check for function calls inside functions called inside pCFunctionType
        exploreFunctionCalls(
            pFunctionCallHierarchy,
            pFunctionThreadIdMap,
            newCFunctionType,
            pVisitedCFunctionTypes,
            pThreadId);
      }
    }
    return pFunctionThreadIdMap;
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

  // TODO create function for functionCallEdgeNodes.getOrDefault(pCurrentNode, pFunctionReturnNode)?

  // Helpers =======================================================================================

  /**
   * Searches the given Set of MPORThreads for the given pPthreadT object.
   *
   * @param pThreads the set of MPORThreads to be searched
   * @param pPthreadT the pthread_t object
   * @return the MPORThread object with pPthreadT as its threadObject (pthread_t)
   * @throws IllegalArgumentException if no thread exists in the set whose threadObject is pPthreadT
   */
  public static MPORThread getThreadByPthreadT(Set<MPORThread> pThreads, CIdExpression pPthreadT) {
    for (MPORThread thread : pThreads) {
      if (thread.threadObject.isPresent()) {
        if (thread.threadObject.orElseThrow().equals(pPthreadT)) {
          return thread;
        }
      }
    }
    throw new IllegalArgumentException("no MPORThread with pPthreadT found in pThreads");
  }
}
