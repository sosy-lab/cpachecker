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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.GAPNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.tests.MPORTests;
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
    Map<MPORThread, CFANode> initFunctionReturnNodes = getInitialFunctionReturnNodes(initState);

    handleState(initState, initFunctionReturnNodes);

    // TODO
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  // TODO sleep set?
  /**
   * Recursively searches all possible transitions between MPORStates, i.e. interleavings.
   *
   * @param pCurrentState the current MPORState we analyze
   * @param pFunctionReturnNodes the map of MPORThreads to their current FunctionReturnNodes
   */
  private void handleState(MPORState pCurrentState, Map<MPORThread, CFANode> pFunctionReturnNodes)
      throws CPATransferException, InterruptedException {

    // make sure the MPORState was not yet visited to prevent infinite loops
    if (!existingStates.contains(pCurrentState)) {
      existingStates.add(pCurrentState);

      // TODO handle preferenceOrders of MPORState here (execute all preceding edges first)
      //  (and think about interleaving preferenceOrders)

      PredicateAbstractState currentAbstractState = pCurrentState.abstractState;

      // for all threads, find the next global access preceding (= GAP) node(s)
      ImmutableSet.Builder<GAPNode> gapNodeBuilder = ImmutableSet.builder();
      for (var threadNode : pCurrentState.threadNodes.entrySet()) {
        MPORThread currentThread = threadNode.getKey();
        // TODO if we don't actually use the abstract state, we might as well just use the initial
        //  AbstractState for the commutativity check?
        currentAbstractState =
            findGapNodes(
                gapNodeBuilder,
                new HashSet<>(),
                threadNode.getValue(),
                pFunctionReturnNodes.get(currentThread),
                currentAbstractState,
                currentThread);
      }
      ImmutableSet<GAPNode> gapNodes = gapNodeBuilder.build();

      // TODO using an immutable map builder here stops the recursion after a few runs?
      // for all global accesses found, map them to their respective GAPNode
      Map<CFAEdge, GAPNode> globalAccessesMap = new HashMap<>();
      for (GAPNode gapNode : gapNodes) {
        CFANode functionReturnNode = gapNode.functionReturnNode;
        // update the functionReturnNode to the one found in findGlobalAccessPrecedingNodes
        pFunctionReturnNodes.put(gapNode.thread, functionReturnNode);
        // create and visit a new state for each global variable access edge
        for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(gapNode.node, functionReturnNode)) {
          checkState(gac.hasGlobalAccess(cfaEdge)); // always held in tests
          globalAccessesMap.put(cfaEdge, gapNode);
        }
      }
      ImmutableMap<CFAEdge, GAPNode> globalAccesses = ImmutableMap.copyOf(globalAccessesMap);

      // TODO remove later
      MPORTests.testCommutativity(logManager, ptr, currentAbstractState, globalAccesses);

      // TODO include commutativity here (double loop)
      //  for all pairs of globalAccesses, find out if they ALL commute
      //  if any pair does not commute, create all possible combinations of globalAccesses and
      //  create if (...) { combination... } else if (...) { combination... } ... else
      //  { combination... } in the sequentialization
      // for all global accesses executed by the threads, create new states to explore
      for (var entry : globalAccesses.entrySet()) {
        // TODO add seq edge here
        CFAEdge executedEdge = entry.getKey();
        MPORThread executingThread = entry.getValue().thread;
        MPORState nextState =
            createUpdatedState(
                pCurrentState.threadNodes, currentAbstractState, executingThread, executedEdge);
        handleState(
            nextState, updateFunctionReturnNodes(nextState.threadNodes, pFunctionReturnNodes));
      }
    }
  }

  /**
   * Recursively finds all global access preceding (GAP) nodes (i.e. CFANodes whose leaving edges
   * read / write a global variable) reachable from the initial value of pCurrentNode. Successors of
   * nodes before global accesses are not considered, i.e. we consider all paths from pCurrentNode
   * containing exactly one global variable access.
   *
   * @param pGapNodeBuilder the ImmutableSet builder we put the found GAPNodes in
   * @param pVisitedNodes the set of already visited CFANodes to prevent infinite loops
   * @param pCurrentNode the current CFANode whose leaving edges successor nodes we analyze
   * @param pFunctionReturnNode the current FunctionReturnNode of the thread
   * @param pAbstractState the current PredicateAbstractState of the program (i.e. of all threads)
   * @param pThread the executing thread
   */
  private PredicateAbstractState findGapNodes(
      ImmutableSet.Builder<GAPNode> pGapNodeBuilder,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode,
      PredicateAbstractState pAbstractState,
      final MPORThread pThread)
      throws CPATransferException, InterruptedException {

    // TODO create a generic method that checks if the set contains an element, adds it if not
    //  present and returns true if the set was modified (we use this pattern a lot here)
    if (!pVisitedNodes.contains(pCurrentNode)) {
      pVisitedNodes.add(pCurrentNode);
      // TODO add seq node
      // if the thread has terminated, stop recursion
      if (!pCurrentNode.equals(pThread.exitNode)) {
        for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
          if (gac.hasGlobalAccess(cfaEdge)) {
            pGapNodeBuilder.add(new GAPNode(pCurrentNode, pFunctionReturnNode, pThread));
            // not using break if for any reason the other leaving edge(s) don't access global vars
          } else {
            // TODO add seq edge
            findGapNodes(
                pGapNodeBuilder,
                pVisitedNodes,
                // this method runs on inputCfa, clonedNodes are only used in the sequentialization
                cfaEdge.getSuccessor(),
                updateFunctionReturnNode(pCurrentNode, pFunctionReturnNode),
                MPORUtil.getNextPredicateAbstractState(ptr, pAbstractState, cfaEdge),
                pThread);
          }
        }
      }
    }
    return pAbstractState;
  }

  private final ConfigurableProgramAnalysis cpa;

  private final LogManager logManager;

  private final Configuration configuration;

  private final ShutdownNotifier shutdownNotifier;

  private final Specification specification;

  private final CFA inputCfa;

  private final GlobalAccessChecker gac;

  private final Sequentialization seq;

  private final PredicateTransferRelation ptr;

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

    cpa = pCpa;
    configuration = pConfiguration;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;
    inputCfa = pInputCfa;

    checkForCProgram(inputCfa);
    checkForParallelProgram(inputCfa);

    gac = new GlobalAccessChecker();
    seq =
        new Sequentialization(
            configuration, logManager, inputCfa, inputCfa.getMainFunction().getFunction());
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, PredicateRefiner.class);
    ptr = predicateCpa.getTransferRelation();

    functionCallMap = getFunctionCallMap(inputCfa);
    threads = getThreads(inputCfa, functionCallMap);
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
    ImmutableMap.Builder<CFANode, CFANode> rFunctionCallMap = ImmutableMap.builder();
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (cfaEdge instanceof FunctionCallEdge functionCallEdge) {
        rFunctionCallMap.put(functionCallEdge.getPredecessor(), functionCallEdge.getReturnNode());
      }
    }
    return rFunctionCallMap.buildOrThrow();
  }

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

    if (!pThreadNodes.contains(pCurrentNode)) {
      pThreadNodes.add(pCurrentNode);
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

    if (pVisitedNodes.contains(pCurrentNode) || pCurrentNode.equals(pThreadExitNode)) {
      return;
    }
    pVisitedNodes.add(pCurrentNode);
    for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
      if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_CREATE)) {
        pEdgesTrace.add(cfaEdge);
        CExpression pthreadT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
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

    if (pVisitedNodes.contains(pCurrentNode) || pCurrentNode.equals(pThreadExitNode)) {
      return;
    }
    pVisitedNodes.add(pCurrentNode);
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
    if (!pMutexNodes.contains(pCurrentNode)) {
      pMutexNodes.add(pCurrentNode);
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

    if (pVisitedNodes.contains(pCurrentNode) || pCurrentNode.equals(pThreadExitNode)) {
      return;
    }
    pVisitedNodes.add(pCurrentNode);
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
    if (pVisitedNodes.contains(pCurrentNode) || pCurrentNode.equals(pThread.exitNode)) {
      return;
    }
    pVisitedNodes.add(pCurrentNode);
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
      // TODO getCreatePreferenceOrders (e.g. main thread: all edges before the first pthread
      //  creation are precedingEdges to the subsequentEdge pthread_create
      rPreferenceOrders.addAll(getMutexPreferenceOrders(pThreadNodes, currentThread, currentNode));
      rPreferenceOrders.addAll(getJoinPreferenceOrders(pThreadNodes, currentThread, currentNode));
      // TODO getBarrierPreferenceOrders
    }
    return rPreferenceOrders.build();
  }

  /**
   * @param pThreadNodes the threads and their current CFANodes
   * @param pCurrentThread the thread where we check if it is calling pthread_create
   * @param pCurrentNode the current CFANode of pCurrentThread
   * @return the set of PreferenceOrders induced by pthread_create calls
   */
  private ImmutableSet<PreferenceOrder> getCreatePreferenceOrder(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      MPORThread pCurrentThread,
      CFANode pCurrentNode) {

    ImmutableSet.Builder<PreferenceOrder> rCreatePreferenceOrders = ImmutableSet.builder();

    // if any thread is at their entryNode
    for (var entry : pThreadNodes.entrySet()) {
      MPORThread thread = entry.getKey();
      if (!thread.equals(pCurrentThread)) {
        if (entry.getValue().equals(thread.entryNode)) {
          // check if pCurrentThread creates the thread which is at its entry
          for (MPORCreate create : pCurrentThread.creates) {
            if (create.pthreadT.equals(thread.threadObject.orElseThrow())) {
              // TODO refactor PreferenceOrder to contain a set of precedingEdges
              FluentIterable<CFAEdge> subsequentEdges = CFAUtils.leavingEdges(thread.entryNode);
              assert subsequentEdges.size() == 1; // assume the entry of a thread is deterministic
              rCreatePreferenceOrders.add(
                  new PreferenceOrder(create.precedingEdges, subsequentEdges.get(0)));
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
   * @param pCurrentThread the thread where we check if it is inside a mutex lock
   * @param pCurrentNode the current CFANode of pCurrentThread
   * @return the set of PreferenceOrders induced by mutex locks
   */
  private ImmutableSet<PreferenceOrder> getMutexPreferenceOrders(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      MPORThread pCurrentThread,
      CFANode pCurrentNode) {

    ImmutableSet.Builder<PreferenceOrder> rMutexPreferenceOrders = ImmutableSet.builder();

    // if pCurrentThread is in a mutex lock
    for (MPORMutex mutex : pCurrentThread.mutexes) {
      if (mutex.nodes.contains(pCurrentNode)) {

        // search all other threads for pthread_mutex_lock calls to the same pthread_mutex_t object
        for (var entry : pThreadNodes.entrySet()) {
          if (!entry.getKey().equals(pCurrentThread)) {
            CFANode otherNode = entry.getValue();
            for (CFAEdge cfaEdge : CFAUtils.leavingEdges(otherNode)) {
              if (FunctionType.isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_MUTEX_LOCK)) {
                CExpression pthreadMutexT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
                if (pthreadMutexT.equals(mutex.pthreadMutexT)) {

                  // extract all CFAEdges inside mutex excluding the leaving edges of exitNodes
                  ImmutableSet.Builder<CFAEdge> precedingEdges = ImmutableSet.builder();
                  precedingEdges.addAll(mutex.edges);
                  rMutexPreferenceOrders.add(new PreferenceOrder(precedingEdges.build(), cfaEdge));
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
   * @param pCurrentThread the thread where we check if it is calling pthread_join
   * @param pCurrentNode the current CFANode of pCurrentThread
   * @return the set of PreferenceOrders induced by joins
   */
  private ImmutableSet<PreferenceOrder> getJoinPreferenceOrders(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      MPORThread pCurrentThread,
      CFANode pCurrentNode) {

    ImmutableSet.Builder<PreferenceOrder> rJoinPreferenceOrders = ImmutableSet.builder();
    // if pCurrentThread is right before a pthread_join call
    for (MPORJoin join : pCurrentThread.joins) {
      if (pCurrentNode.equals(join.preJoinNode)) {
        CExpression pthreadT = CFAUtils.getParameterAtIndex(join.joinEdge, 0);
        MPORThread targetThread = getThreadByPthreadT(pThreadNodes, pthreadT);
        // if the thread specified as pthread_t in the pthread_join call has not yet terminated
        CFANode targetThreadNode = pThreadNodes.get(targetThread);
        assert targetThreadNode != null;
        if (!targetThreadNode.equals(targetThread.exitNode)) {
          // add all CFAEdges executed by pthread_t as preceding edges
          ImmutableSet<CFAEdge> precedingEdges = targetThread.edges;
          rJoinPreferenceOrders.add(new PreferenceOrder(precedingEdges, join.joinEdge));
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
    ImmutableMap.Builder<MPORThread, CFANode> threadNodesBuilder = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      threadNodesBuilder.put(thread, thread.entryNode);
    }
    ImmutableMap<MPORThread, CFANode> threadNodes = threadNodesBuilder.buildOrThrow();
    return new MPORState(
        threadNodes,
        getPreferenceOrdersForThreadNodes(threadNodes),
        AbstractStates.extractStateByType(initAbstractState, PredicateAbstractState.class));
  }

  /**
   * Creates the initial map of FunctionReturnNodes.
   *
   * @param pInitState the initial MPORState of the program, i.e. threads at their start routine /
   *     main FunctionEntryNode
   * @return the mapping of MPORThreads to their initial FunctionReturnNodes
   */
  private Map<MPORThread, CFANode> getInitialFunctionReturnNodes(MPORState pInitState) {
    Map<MPORThread, CFANode> rFunctionReturnNodes = new HashMap<>();
    for (var entry : pInitState.threadNodes.entrySet()) {
      MPORThread currentThread = entry.getKey();
      CFANode currentNode = entry.getValue();
      rFunctionReturnNodes.put(currentThread, updateFunctionReturnNode(currentNode, null));
    }
    return rFunctionReturnNodes;
  }

  /**
   * Updates pFunctionReturnNodes based on pThreadNodes.
   *
   * @param pThreadNodes the current CFANodes for all MPORThreads
   * @param pFunctionReturnNodes the current FunctionReturnNodes for all threads to be updated
   */
  private Map<MPORThread, CFANode> updateFunctionReturnNodes(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      Map<MPORThread, CFANode> pFunctionReturnNodes) {

    for (var entry : pThreadNodes.entrySet()) {
      MPORThread thread = entry.getKey();
      if (pFunctionReturnNodes.containsKey(thread)) {
        CFANode currentNode = entry.getValue();
        pFunctionReturnNodes.put(
            thread, updateFunctionReturnNode(currentNode, pFunctionReturnNodes.get(thread)));
      } else {
        throw new IllegalArgumentException(
            "invalid pState pFunctionReturnNodes pairing, they must"
                + " contain the same MPORThreads");
      }
    }
    return pFunctionReturnNodes;
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
   * returned. If not, we take the previous pFunctionReturnNode or reset it to null if pCurrentNode
   * is a FunctionExitNode, i.e. the previous pFunctionReturnNode is not relevant anymore in the
   * next iteration.
   *
   * @param pCurrentNode in recursive functions that search the leaving CFAEdges of the current
   *     node, the previous node of the analyzed node should be used here
   * @param pFunctionReturnNode the previous pFunctionReturnNode
   * @return the previous or new FunctionReturnNode or null if pCurrentNode exits a function
   */
  private CFANode updateFunctionReturnNode(CFANode pCurrentNode, CFANode pFunctionReturnNode) {
    return functionCallMap.getOrDefault(
        pCurrentNode,
        // reset the FunctionReturnNode when encountering a FunctionExitNode
        pCurrentNode instanceof FunctionExitNode ? null : pFunctionReturnNode);
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

  /**
   * Returns a new state with the same threadNodes map except that the key pThread is assigned the
   * successor CFANode of pExecutedEdge.
   *
   * @param pThreadNodes The previous mapping of threads to their current nodes
   * @param pAbstractState The current PredicateAbstractState
   * @param pThread The MPORThread that has a new CFANode (= state)
   * @param pExecutedEdge The CFAEdge executed by pThread
   * @return MPORState with CFANode of pThread being the successor of pExecutedEdge
   */
  private MPORState createUpdatedState(
      @NonNull ImmutableMap<MPORThread, CFANode> pThreadNodes,
      @NonNull PredicateAbstractState pAbstractState,
      @NonNull MPORThread pThread,
      @NonNull CFAEdge pExecutedEdge)
      throws CPATransferException, InterruptedException {

    checkNotNull(pThreadNodes);
    checkNotNull(pAbstractState);
    checkNotNull(pThread);
    checkNotNull(pExecutedEdge);
    checkArgument(pThreadNodes.containsKey(pThread), "threadNodes must contain pThread");

    // create the threadNodes map for the updatedState
    ImmutableMap.Builder<MPORThread, CFANode> threadNodesBuilder = ImmutableMap.builder();
    for (var entry : pThreadNodes.entrySet()) {
      if (!entry.getKey().equals(pThread)) {
        threadNodesBuilder.put(entry);
      }
    }
    threadNodesBuilder.put(pThread, pExecutedEdge.getSuccessor());

    ImmutableMap<MPORThread, CFANode> newThreadNodes = threadNodesBuilder.buildOrThrow();
    PredicateAbstractState newAbstractState =
        MPORUtil.getNextPredicateAbstractState(ptr, pAbstractState, pExecutedEdge);

    // for optimization and preventing infinite loops, search existing states for threadNodes
    for (MPORState rExistingState : existingStates) {
      // not checking pathFormulas here, their semantics are only relevant for commutativity
      if (rExistingState.areThreadNodesEqual(newThreadNodes)) {
        return rExistingState;
      }
    }

    // otherwise, return a new MPORState object and (costly) initialize variables
    return new MPORState(
        newThreadNodes, getPreferenceOrdersForThreadNodes(newThreadNodes), newAbstractState);
  }
}
