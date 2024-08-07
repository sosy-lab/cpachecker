// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORCreate;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORJoin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.PreferenceOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.ExecutionTrace;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.MPORState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * This is an implementation of a Partial Order Reduction (POR) algorithm, presented in the 2022
 * paper "Sound Sequentialization for Concurrent Program Verification". This algorithm aims at
 * producing a reduced sequentialization of a parallel C program. The reduced sequentialization can
 * be given to an existing verifier capable of verifying sequential C programs. The POR and the
 * verifier serve as modules, hence MPOR (Modular Partial Order Reduction).
 *
 * <p>Restrictions:
 *
 * <ul>
 *   <li>Using an unbounded number of threads (i.e. any loop for, while { pthread_create... }) is
 *       undefined
 *   <li>The input program must be a C program
 *   <li>The input program uses POSIX threads (pthreads)
 *   <li>The start routines / main function of all pthreads / the main thread must contain a
 *       FunctionExitNode
 * </ul>
 */
@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  // TODO remove all @SuppressWarnings once finished

  // TODO (not sure if important for our algorithm) PredicateAbstractState.abstractLocations
  //  contains all CFANodes visited so far

  // TODO shorten all functionReturnNodes to funcReturnNodes

  /**
   * The number of {@link CFAEdge}s to be considered at the end of two {@link ExecutionTrace}s to be
   * approximated as equivalent. Increasing this value is a major source of inefficiency.
   */
  public static final int EXECUTION_TRACE_TAIL_SIZE = 1;

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    // TODO the number of total interleavings under full commutativity is given by
    //  multinomial coefficient of ( n / k1, k2, ..., km)
    //  with n the total number of edges in the program
    //  and k1, k2, ..., km the number of edges for each thread 1...m

    checkForCorrectInitialState(pReachedSet, threads);

    // if there is only one element in pReachedSet, it is our initial AbstractState
    PredicateAbstractState initAbstractState =
        AbstractStates.extractStateByType(
            pReachedSet.asCollection().iterator().next(), PredicateAbstractState.class);
    MPORState initState = getInitialState(threads, initAbstractState);

    handleState(initState);
    // TODO
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /**
   * Recursively searches all possible transitions between MPORStates, i.e. interleavings, factoring
   * in (positional) preference orders and local vs. global accesses.
   *
   * @param pState the current MPORState we analyze
   */
  private void handleState(MPORState pState) throws CPATransferException, InterruptedException {

    // make sure the MPORState was not yet visited to prevent infinite loops
    if (MPORUtil.shouldVisit(existingStates, pState)) {

      // TODO create handlePreferenceOrders function that returns an updated MPORState

      // execute all threads up to a global access preceding (GAP) node
      ImmutableSet.Builder<MPORState> stateBuilder = ImmutableSet.builder();
      Map<MPORThread, Set<CFANode>> visitedNodes = new HashMap<>();
      for (MPORThread thread : pState.threadNodes.keySet()) {
        visitedNodes.put(thread, new HashSet<>());
      }
      ImmutableSet<MPORState> gapStates = findGapStates(stateBuilder, visitedNodes, pState);

      // TODO include commutativity here (double loop)
      //  for all pairs of globalAccesses, find out if they ALL commute
      // for all global accesses executed by the threads, create new states to explore
      for (MPORState gapState : gapStates) {
        for (var entry : gapState.threadNodes.entrySet()) {
          MPORThread thread = entry.getKey();
          CFANode threadNode = entry.getValue();
          CFANode funcReturnNode = gapState.functionReturnNodes.get(thread);
          for (CFAEdge globalEdge : contextSensitiveLeavingEdges(threadNode, funcReturnNode)) {
            handleState(createUpdatedState(gapState, thread, globalEdge));
          }
        }
      }
    }
  }

  /**
   * Recursively executes the threads in pState up to a global access preceding (GAP) node (i.e.
   * CFANodes whose leaving edge(s) read / write a global variable) reachable from the initial
   * threadNodes of pState.
   *
   * <p>The threads are executed in a round-robin fashion, allowing us to add states to
   * pGapStatesBuilder if all threads reached a GAP node in every iteration. If a GAPNode features
   * local / global mixed leaving edges, the algorithm can continue from the found GAP state, and
   * possibly create a new GAP state, covering all paths.
   *
   * <p>GAP states can be reached non-deterministically, so we return a set of GAP states.
   *
   * @param pGapStatesBuilder the builder where we store the found GAP states
   * @param pVisitedNodes the set of already visited CFANodes for each thread to prevent infinite
   *     loops
   * @param pState the global state of the program (all threads)
   * @return the set of found GAP states
   */
  private ImmutableSet<MPORState> findGapStates(
      ImmutableSet.Builder<MPORState> pGapStatesBuilder,
      Map<MPORThread, Set<CFANode>> pVisitedNodes,
      MPORState pState)
      throws CPATransferException, InterruptedException {

    if (allThreadsAtGapNode(pState)) {
      pGapStatesBuilder.add(pState);
    }
    // for each thread and its current node
    for (var entry : pState.threadNodes.entrySet()) {
      MPORThread currentThread = entry.getKey();
      CFANode currentNode = entry.getValue();
      if (MPORUtil.shouldVisit(pVisitedNodes.get(currentThread), currentNode)) {
        // if the thread has terminated, stop
        if (!currentNode.equals(currentThread.exitNode)) {
          CFANode funcReturnNode = pState.functionReturnNodes.get(currentThread);
          ImmutableSet<CFAEdge> contextEdges =
              contextSensitiveLeavingEdges(currentNode, funcReturnNode);
          for (CFAEdge cfaEdge : contextEdges) {
            // if the next edge(s) is a global access, stop
            if (!GAC.hasGlobalAccess(cfaEdge)) {
              // otherwise, continue executing thread until a global access
              MPORState nextState = createUpdatedState(pState, currentThread, cfaEdge);
              findGapStates(pGapStatesBuilder, pVisitedNodes, nextState);
            }
          }
        }
      }
    }
    return pGapStatesBuilder.build();
  }

  private final ConfigurableProgramAnalysis CPA;

  private final LogManager LOG_MANAGER;

  private final Configuration CONFIG;

  private final ShutdownNotifier SHUTDOWN_NOTIFIER;

  private final Specification SPECIFICATION;

  private final CFA INPUT_CFA;

  private final GlobalAccessChecker GAC;

  private final Sequentialization SEQ;

  private final PredicateTransferRelation PTR;

  /**
   * A map from FunctionCallEdge Predecessors to Return Nodes. Needs to be initialized before {@link
   * MPORAlgorithm#threads}.
   */
  private final ImmutableMap<CFANode, CFANode> functionCallMap;

  /**
   * The set of threads in the program, including the main thread and all pthreads. Needs to be
   * initialized after {@link MPORAlgorithm#functionCallMap}.
   */
  private final ImmutableSet<MPORThread> threads;

  /**
   * The set of already existing states, used to prevent the creation of semantically equivalent
   * states.
   */
  private final Set<MPORState> existingStates;

  public MPORAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pInputCfa)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    CPA = pCpa;
    CONFIG = pConfiguration;
    LOG_MANAGER = pLogManager;
    SHUTDOWN_NOTIFIER = pShutdownNotifier;
    SPECIFICATION = pSpecification;
    INPUT_CFA = pInputCfa;

    checkForCProgram(INPUT_CFA);
    checkForParallelProgram(INPUT_CFA);

    GAC = new GlobalAccessChecker();
    SEQ =
        new Sequentialization(
            CONFIG, LOG_MANAGER, INPUT_CFA, INPUT_CFA.getMainFunction().getFunction());
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(CPA, PredicateCPA.class, PredicateRefiner.class);
    PTR = predicateCpa.getTransferRelation();

    functionCallMap = getFunctionCallMap(INPUT_CFA);
    threads = getThreads(INPUT_CFA, functionCallMap);
    existingStates = new HashSet<>();
  }

  // Preconditions ===============================================================================

  /** Checks whether the input language of the program is C and throws an exception if not. */
  private void checkForCProgram(CFA pCfa) {
    checkArgument(
        pCfa.getMetadata().getInputLanguage().equals(Language.C), "MPOR expects C program");
  }

  /**
   * Checks whether any edge in pCfa contains a pthread_create call. If that is not the case, the
   * algorithm ends and the user is informed that MPOR is meant to analyze parallel programs.
   */
  private void checkForParallelProgram(CFA pCfa) {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_CREATE)) {
        isParallel = true;
        break;
      }
    }
    checkArgument(
        isParallel, "MPOR expects parallel C program with at least one pthread_create call");
  }

  /**
   * Checks if the initial ReachedSet is correct.
   *
   * @param pReachedSet the initial ReachedSet in {@link MPORAlgorithm#run}
   * @param pThreads the set of MPORThreads in the program
   * @throws IllegalArgumentException if the ReachedSet's length is not 1 or if the LocationNode of
   *     the initial AbstractState is not the main FunctionEntryNode
   */
  private void checkForCorrectInitialState(
      ReachedSet pReachedSet, ImmutableSet<MPORThread> pThreads) {

    boolean oneInitialState = pReachedSet.asCollection().size() == 1;
    checkArgument(oneInitialState, "the initial ReachedSet should contain only one AbstractState");

    FunctionEntryNode mainFunctionEntryNode = getMainThread(pThreads).entryNode;
    boolean correctMainFunctionEntryNode = false;
    // take the first AbstractState, there is only one anyway
    if (pReachedSet.asCollection().iterator().next() instanceof ARGState argState) {
      // extract the LocationState containing the LocationNode of the initial AbstractState
      if (argState.getWrappedState() instanceof CompositeState compositeState) {
        for (AbstractState abstractState : compositeState.getWrappedStates()) {
          if (abstractState instanceof LocationState locationState) {
            correctMainFunctionEntryNode =
                mainFunctionEntryNode.equals(locationState.getLocationNode());
          }
        }
      }
    }
    checkArgument(
        correctMainFunctionEntryNode,
        "the initial AbstractState's location is not the main FunctionEntryNode");
  }

  // Variable Initializers =======================================================================

  /**
   * Searches all CFAEdges in pCfa for FunctionCallEdges and maps the predecessor CFANodes to their
   * ReturnNodes so that context-sensitive algorithms can be performed on the CFA.
   *
   * <p>E.g. a FunctionExitNode may have several leaving Edges, one for each time the function is
   * called. With the Map, extracting only the leaving Edge resulting in the ReturnNode is possible.
   * Using FunctionEntryNodes is not possible because the calling context (the node before the
   * function call) is lost, which is why keys are not FunctionEntryNodes.
   *
   * @param pCfa the CFA to be analyzed
   * @return A Map of CFANodes before a FunctionCallEdge (keys) to the CFANodes where a function
   *     continues (values, i.e. the ReturnNode) after going through the CFA of the function called.
   */
  private ImmutableMap<CFANode, CFANode> getFunctionCallMap(CFA pCfa) {
    Builder<CFANode, CFANode> rFunctionCallMap = ImmutableMap.builder();
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (cfaEdge instanceof FunctionCallEdge functionCallEdge) {
        rFunctionCallMap.put(functionCallEdge.getPredecessor(), functionCallEdge.getReturnNode());
      }
    }
    return rFunctionCallMap.buildOrThrow();
  }

  // TODO create MPORThreadBuilder that initializes all these variables?
  // TODO pthread_create calls in loops can be considered by loop unrolling
  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * <p>This functions needs to be called after functionCallMap was initialized so that we can track
   * the calling context of each thread.
   *
   * @param pCfa the CFA to be analyzed
   * @param pFunctionCallMap map from CFANodes before FunctionCallEdges to FunctionReturnNodes
   * @return the set of threads
   */
  private ImmutableSet<MPORThread> getThreads(
      CFA pCfa, ImmutableMap<CFANode, CFANode> pFunctionCallMap) {

    ImmutableSet.Builder<MPORThread> rThreads = ImmutableSet.builder();

    // add the main thread
    FunctionEntryNode mainEntryNode = pCfa.getMainFunction();
    FunctionExitNode mainExitNode = MPORUtil.getFunctionExitNode(mainEntryNode);
    rThreads.add(createThread(Optional.empty(), mainEntryNode, mainExitNode));

    // search the CFA for pthread_create calls
    for (CFAEdge cfaEdge : CFAUtils.allUniqueEdges(pCfa)) {
      if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_CREATE)) {
        // extract the first parameter of pthread_create, i.e. the pthread_t value
        CExpression pthreadT =
            CFAUtils.getValueFromPointer(CFAUtils.getParameterAtIndex(cfaEdge, 0));
        // extract the third parameter of pthread_create which points to the start routine function
        CFunctionType startRoutine =
            CFAUtils.getCFunctionTypeFromCExpression(CFAUtils.getParameterAtIndex(cfaEdge, 2));
        FunctionEntryNode entryNode =
            CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, startRoutine);
        FunctionExitNode exitNode = MPORUtil.getFunctionExitNode(entryNode);
        rThreads.add(createThread(Optional.ofNullable(pthreadT), entryNode, exitNode));
      }
    }
    return rThreads.build();
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   *
   * @param pPthreadT the pthread_t object, set to empty for the main thread
   * @param pEntryNode the entry node of the start routine or main function of the thread
   * @param pExitNode the exit node of the start routine or main function of the thread
   * @return a MPORThread object with properly initialized variables
   */
  private MPORThread createThread(
      Optional<CExpression> pPthreadT, FunctionEntryNode pEntryNode, FunctionExitNode pExitNode) {

    Set<CFANode> threadNodes = new HashSet<>(); // using set so that we can use .contains()
    ImmutableSet.Builder<CFAEdge> threadEdges = ImmutableSet.builder();
    initThreadVariables(pExitNode, threadNodes, threadEdges, pEntryNode, null);

    ImmutableSet.Builder<MPORCreate> creates = ImmutableSet.builder();
    searchThreadForCreates(creates, pExitNode, new HashSet<>(), new HashSet<>(), pEntryNode, null);

    ImmutableSet.Builder<MPORMutex> mutexes = ImmutableSet.builder();
    searchThreadForMutexes(mutexes, pExitNode, new HashSet<>(), pEntryNode, null);

    ImmutableSet.Builder<MPORJoin> joins = ImmutableSet.builder();
    searchThreadForJoins(joins, pExitNode, new HashSet<>(), pEntryNode, null);

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
   * @param pFunctionReturnNode pFunctionReturnNode used to track the original context when entering
   *     the CFA of another function. see {@link MPORAlgorithm#getFunctionCallMap(CFA)} for more
   *     info.
   */
  private void initThreadVariables(
      final FunctionExitNode pExitNode,
      Set<CFANode> pThreadNodes, // set so that we can use .contains
      ImmutableSet.Builder<CFAEdge> pThreadEdges,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    if (MPORUtil.shouldVisit(pThreadNodes, pCurrentNode)) {
      if (!pCurrentNode.equals(pExitNode)) {
        for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
          pThreadEdges.add(cfaEdge);
          initThreadVariables(
              pExitNode,
              pThreadNodes,
              pThreadEdges,
              cfaEdge.getSuccessor(),
              updateFunctionReturnNode(pCurrentNode, pFunctionReturnNode));
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
   * @param pFunctionReturnNode used to track the original context when entering the CFA of another
   *     function. see {@link MPORAlgorithm#getFunctionCallMap(CFA)} for more info.
   */
  private void searchThreadForCreates(
      ImmutableSet.Builder<MPORCreate> pCreates,
      final CFANode pThreadExitNode,
      Set<CFANode> pVisitedNodes,
      Set<CFAEdge> pEdgesTrace,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    if (!pCurrentNode.equals(pThreadExitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
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
              updateFunctionReturnNode(pCurrentNode, pFunctionReturnNode));
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
   * @param pFunctionReturnNode used to track the original context when entering the CFA of another
   *     function. see {@link MPORAlgorithm#getFunctionCallMap(CFA)} for more info.
   */
  private void searchThreadForMutexes(
      ImmutableSet.Builder<MPORMutex> pMutexes,
      final CFANode pThreadExitNode,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    if (!pCurrentNode.equals(pThreadExitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
          if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_MUTEX_LOCK)) {
            CExpression pthreadMutexT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
            // the successor node of mutex_lock is the first inside the lock
            CFANode initialNode = cfaEdge.getSuccessor();
            Set<CFANode> mutexNodes = new HashSet<>(); // using a set so that we can use .contains
            ImmutableSet.Builder<CFAEdge> mutexEdges = ImmutableSet.builder();
            ImmutableSet.Builder<CFANode> mutexExitNodes = ImmutableSet.builder();
            initMutexVariables(
                pthreadMutexT, mutexNodes, mutexEdges, mutexExitNodes, initialNode, null);
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
              updateFunctionReturnNode(pCurrentNode, pFunctionReturnNode));
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
   * @param pFunctionReturnNode used to track the original context when entering the CFA of another
   *     function. see {@link MPORAlgorithm#getFunctionCallMap(CFA)} for more info.
   */
  private void initMutexVariables(
      final CExpression pPthreadMutexT,
      Set<CFANode> pMutexNodes, // using a set so that we can use .contains(...)
      ImmutableSet.Builder<CFAEdge> pMutexEdges,
      ImmutableSet.Builder<CFANode> pMutexExitNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    // visit CFANodes only once to prevent infinite loops in case of loop structures
    if (MPORUtil.shouldVisit(pMutexNodes, pCurrentNode)) {
      for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
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
              updateFunctionReturnNode(pCurrentNode, pFunctionReturnNode));
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
   * @param pFunctionReturnNode used to track the original context when entering the CFA of another
   *     function. see {@link MPORAlgorithm#getFunctionCallMap(CFA)} for more info.
   */
  private void searchThreadForJoins(
      ImmutableSet.Builder<MPORJoin> pJoins,
      final CFANode pThreadExitNode,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    if (!pCurrentNode.equals(pThreadExitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
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
              updateFunctionReturnNode(pCurrentNode, pFunctionReturnNode));
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
   * @param pFunctionReturnNode TODO
   */
  private void searchThreadForBarriers(
      MPORThread pThread,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    // TODO see pthread-divine for example barrier programs
    if (!pCurrentNode.equals(pThread.exitNode)) {
      if (MPORUtil.shouldVisit(pVisitedNodes, pCurrentNode)) {
        for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
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
              updateFunctionReturnNode(pCurrentNode, pFunctionReturnNode));
        }
      }
    }
  }

  // Preference Orders ===========================================================================

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
        if (entry.getValue().equals(createdThread.entryNode)) {
          // check if pCreatingThread creates the thread which is at its entry
          for (MPORCreate create : pCreatingThread.creates) {
            if (create.createdPthreadT.equals(createdThread.threadObject.orElseThrow())) {
              ImmutableSet<CFAEdge> subsequentEdges =
                  ImmutableSet.copyOf(CFAUtils.leavingEdges(createdThread.entryNode));
              rCreatePreferenceOrders.add(
                  new PreferenceOrder(
                      pCreatingThread, createdThread, create.precedingEdges, subsequentEdges));
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
    for (MPORMutex mutex : pThreadInMutex.mutexes) {
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
    for (MPORJoin join : pJoiningThread.joins) {
      if (pJoiningNode.equals(join.preJoinNode)) {
        CExpression pthreadT = CFAUtils.getParameterAtIndex(join.joinEdge, 0);
        MPORThread targetThread = getThreadByPthreadT(pThreadNodes, pthreadT);
        // if the thread specified as pthread_t in the pthread_join call has not yet terminated
        CFANode targetThreadNode = pThreadNodes.get(targetThread);
        assert targetThreadNode != null;
        if (!targetThreadNode.equals(targetThread.exitNode)) {
          // add all CFAEdges executed by pthread_t as preceding edges
          ImmutableSet<CFAEdge> precedingEdges = targetThread.edges;
          ImmutableSet<CFAEdge> subsequentEdges =
              ImmutableSet.copyOf(CFAUtils.leavingEdges(join.preJoinNode));
          rJoinPreferenceOrders.add(
              new PreferenceOrder(targetThread, pJoiningThread, precedingEdges, subsequentEdges));
        }
      }
    }
    return rJoinPreferenceOrders.build();
  }

  // (Private) Helpers ===========================================================================

  /**
   * Returns the initial MPORState of the program, properly initializing the map from MPORThreads to
   * their start routines / main FunctionEntryNodes, the PreferenceOrders and the corresponding
   * AbstractState.
   *
   * @param pThreads the set of Threads we put in {@link MPORState#threadNodes}
   * @param initAbstractState the initial AbstractState in {@link MPORAlgorithm#run}
   * @return the initial MPORState of the program
   */
  private MPORState getInitialState(
      ImmutableSet<MPORThread> pThreads, AbstractState initAbstractState) {

    Builder<MPORThread, CFANode> threadNodesBuilder = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      threadNodesBuilder.put(thread, thread.entryNode);
    }
    ImmutableMap<MPORThread, CFANode> threadNodes = threadNodesBuilder.buildOrThrow();

    ImmutableList.Builder<CFAEdge> emptyTrace = ImmutableList.builder();

    return new MPORState(
        threadNodes,
        getInitialFunctionReturnNodes(threadNodes),
        getPreferenceOrdersForThreadNodes(threadNodes),
        new ExecutionTrace(emptyTrace.build()),
        AbstractStates.extractStateByType(initAbstractState, PredicateAbstractState.class));
  }

  /**
   * Creates the initial map of FunctionReturnNodes.
   *
   * @param pInitThreadNodes the map of threads to their main functions / start routines
   *     FunctionEntryNode
   * @return the mapping of MPORThreads to their initial FunctionReturnNodes
   */
  private ImmutableMap<MPORThread, CFANode> getInitialFunctionReturnNodes(
      ImmutableMap<MPORThread, CFANode> pInitThreadNodes) {
    Builder<MPORThread, CFANode> rFunctionReturnNodes = ImmutableMap.builder();
    for (var entry : pInitThreadNodes.entrySet()) {
      MPORThread currentThread = entry.getKey();
      CFANode currentNode = entry.getValue();
      rFunctionReturnNodes.put(currentThread, updateFunctionReturnNode(currentNode, null));
    }
    return rFunctionReturnNodes.buildOrThrow();
  }

  /**
   * Updates pPrevFunctionReturnNodes based on pCurrentThreadNodes.
   *
   * @param pCurrentThreadNodes the current CFANodes for all MPORThreads
   * @param pPrevFunctionReturnNodes the current FunctionReturnNodes for all threads to be updated
   */
  private ImmutableMap<MPORThread, CFANode> updateFunctionReturnNodes(
      ImmutableMap<MPORThread, CFANode> pCurrentThreadNodes,
      ImmutableMap<MPORThread, CFANode> pPrevFunctionReturnNodes) {

    Builder<MPORThread, CFANode> rFunctionReturnNodes = ImmutableMap.builder();
    for (var entry : pCurrentThreadNodes.entrySet()) {
      MPORThread thread = entry.getKey();
      checkArgument(
          pPrevFunctionReturnNodes.containsKey(thread),
          "thread nodes and function return nodes must contain the same threads");
      CFANode currentNode = entry.getValue();
      rFunctionReturnNodes.put(
          thread, updateFunctionReturnNode(currentNode, pPrevFunctionReturnNodes.get(thread)));
    }
    return rFunctionReturnNodes.buildOrThrow();
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

  /**
   * Returns the MPORThread in pThreads whose pthread_t object is null.
   *
   * @param pThreads the set of MPORThreads to be searched
   * @return the main thread in pThreads
   * @throws IllegalArgumentException if the main thread is not found
   */
  private MPORThread getMainThread(ImmutableSet<MPORThread> pThreads) {
    for (MPORThread thread : pThreads) {
      if (thread.isMain()) {
        return thread;
      }
    }
    throw new IllegalArgumentException("pThreads does not contain the main thread");
  }

  /**
   * Searches pFunctionCallMap for pCurrentNode. If the key is present, the FunctionReturnNode is
   * returned. If not, we take the previous pPrevFunctionReturnNode or reset it to null if
   * pCurrentNode is a FunctionExitNode, i.e. the previous pPrevFunctionReturnNode is not relevant
   * anymore in the next iteration.
   *
   * @param pCurrentNode in recursive functions that search the leaving CFAEdges of the current
   *     node, the previous node of the analyzed node should be used here
   * @param pPrevFunctionReturnNode the previous pPrevFunctionReturnNode
   * @return the previous or new FunctionReturnNode or null if pCurrentNode exits a function
   */
  private CFANode updateFunctionReturnNode(CFANode pCurrentNode, CFANode pPrevFunctionReturnNode) {
    return functionCallMap.getOrDefault(
        pCurrentNode,
        // reset the FunctionReturnNode when encountering a FunctionExitNode
        pCurrentNode instanceof FunctionExitNode ? null : pPrevFunctionReturnNode);
  }

  /**
   * Background: a FunctionExitNode may have several leaving Edges, one for each time the function
   * is called. With this function, if pCurrentNode is a FunctionExitNode, we extract only the
   * leaving edges of the original calling context, i.e. the edges whose successor is
   * pFunctionReturnNode.
   *
   * @param pCurrentNode the CFANode whose leaving Edges we analyze
   * @param pFunctionReturnNode the return node (extracted from the original functionCallEdge)
   * @return a FluentIterable of context-sensitive leaving CFAEdges of pCurrentNode
   */
  private ImmutableSet<CFAEdge> contextSensitiveLeavingEdges(
      CFANode pCurrentNode, CFANode pFunctionReturnNode) {

    ImmutableSet.Builder<CFAEdge> rContextSensitiveEdges = ImmutableSet.builder();
    if (pCurrentNode instanceof FunctionExitNode) {
      rContextSensitiveEdges.addAll(
          CFAUtils.leavingEdges(pCurrentNode)
              .filter(cfaEdge -> cfaEdge.getSuccessor().equals(pFunctionReturnNode)));
    } else {
      rContextSensitiveEdges.addAll(CFAUtils.leavingEdges(pCurrentNode));
    }
    return rContextSensitiveEdges.build();
  }

  // TODO create MPORStateBuilder class with static methods and a protected MPORState constructor
  //  also, include the sequentialization nodes and edges in this function
  /**
   * Returns a new state with the same threadNodes map except that the key pThread is assigned the
   * successor CFANode of pExecutedEdge.
   *
   * @param pState the MPORState from which we execute pExecutedEdge
   * @param pThread The MPORThread that has a new CFANode (= state)
   * @param pExecutedEdge The CFAEdge executed by pThread
   * @return MPORState with CFANode of pThread being the successor of pExecutedEdge
   */
  private MPORState createUpdatedState(
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
    ImmutableMap<MPORThread, CFANode> newFunctionReturnNodes =
        updateFunctionReturnNodes(newThreadNodes, pState.functionReturnNodes);
    ExecutionTrace newExecutionTrace = pState.executionTrace.add(pExecutedEdge);

    // to prevent infinite loops, check for a semantically equivalent state
    for (MPORState rExistingState : existingStates) {
      if (areStatesEquivalent(
          rExistingState, newThreadNodes, newFunctionReturnNodes, newExecutionTrace)) {
        return rExistingState;
      }
    }
    return new MPORState(
        newThreadNodes,
        newFunctionReturnNodes,
        getPreferenceOrdersForThreadNodes(newThreadNodes),
        newExecutionTrace,
        MPORUtil.getNextPredicateAbstractState(PTR, pState.abstractState, pExecutedEdge));
  }

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
      ImmutableMap<MPORThread, CFANode> pFunctionReturnNodesB,
      ExecutionTrace pExecutionTraceB) {

    // TODO we should optimize this function by creating orders for the conditions
    //  e.g. if we have 2 threads but the tail is 3 elements, we should check the threads and
    //  funcreturnnodes first. if it is the other way around, checking the tail first is more
    //  performant
    return pStateA.threadNodes.equals(pThreadNodesB)
        && pStateA.functionReturnNodes.equals(pFunctionReturnNodesB)
        && pStateA.executionTrace.tail().equals(pExecutionTraceB.tail());
  }

  /**
   * Checks if all threads in pState are at a CFANode whose leaving edges contains at least one
   * global access.
   *
   * @param pState the global program state to be analyzed
   * @return true if all threadNodes have at least one leaving edge with global access
   */
  private boolean allThreadsAtGapNode(MPORState pState) {
    for (var entry : pState.threadNodes.entrySet()) {
      MPORThread currentThread = entry.getKey();
      CFANode currentNode = entry.getValue();
      CFANode funcReturnNode = pState.functionReturnNodes.get(currentThread);
      ImmutableSet<CFAEdge> contextEdges =
          contextSensitiveLeavingEdges(currentNode, funcReturnNode);
      if (!GAC.anyGlobalAccess(contextEdges)) {
        return false;
      }
    }
    return true;
  }
}
