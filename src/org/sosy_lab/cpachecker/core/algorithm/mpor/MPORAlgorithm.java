// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.ExecutionTrace;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.MPORState;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.StateBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadBuilder;
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

  /**
   * The number of {@link CFAEdge}s to be considered at the end of two {@link ExecutionTrace}s to be
   * approximated as equivalent. Increasing this value is a major source of inefficiency.
   */
  public static final int EXECUTION_TRACE_TAIL_SIZE = 0;

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
    MPORState initState = stateBuilder.createInitState(threads, initAbstractState);

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
    if (MPORUtil.shouldVisit(stateBuilder.getExistingStates(), pState)) {
      // TODO create handlePreferenceOrders function that returns a set of updated MPORStates

      // execute all threads up to a global access preceding (GAP) node
      ImmutableSet.Builder<MPORState> gapStateBuilder = ImmutableSet.builder();
      Map<MPORThread, Set<CFANode>> visitedNodes = new HashMap<>();
      for (MPORThread thread : pState.threadNodes.keySet()) {
        visitedNodes.put(thread, new HashSet<>());
      }
      ImmutableSet<MPORState> gapStates = findGapStates(gapStateBuilder, visitedNodes, pState);

      for (MPORState gapState : gapStates) {
        // if all global accesses commute, execute them sequentially, otherwise create interleaving
        if (allLeavingEdgesCommute(gapState)) {
          for (MPORState nextState : executeGlobalAccessesSequentially(gapState)) {
            handleState(nextState);
          }
        } else {
          // TODO create seq interleaving
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
          Optional<CFANode> funcReturnNode = pState.funcReturnNodes.get(currentThread);
          for (CFAEdge cfaEdge : MPORUtil.returnLeavingEdges(currentNode, funcReturnNode)) {
            // if the next edge(s) is a global access, stop
            if (!GAC.hasGlobalAccess(cfaEdge)) {
              // otherwise, continue executing thread until a global access
              MPORState nextState = stateBuilder.createNewState(pState, currentThread, cfaEdge);
              findGapStates(pGapStatesBuilder, pVisitedNodes, nextState);
            }
          }
        }
      }
    }
    return pGapStatesBuilder.build();
  }

  /**
   * Executes all possible combinations of leaving edges sequentially, i.e. thread order does not
   * matter.
   *
   * @param pGapState the current GAP state where all threads execute at least one global access
   *     next
   * @return the set of reached MPORStates
   */
  private ImmutableSet<MPORState> executeGlobalAccessesSequentially(MPORState pGapState)
      throws CPATransferException, InterruptedException {

    ImmutableSet.Builder<MPORState> nextStates = ImmutableSet.builder();

    // create list of maps of edges to be executed to their threads
    Map<MPORThread, List<CFAEdge>> threadEdges = new HashMap<>();
    for (var entry : pGapState.threadNodes.entrySet()) {
      MPORThread thread = entry.getKey();
      CFANode node = entry.getValue();
      Optional<CFANode> funcReturnNode = pGapState.funcReturnNodes.get(thread);
      threadEdges.put(thread, new ArrayList<>(MPORUtil.returnLeavingEdges(node, funcReturnNode)));
    }

    // TODO make immutable?
    // create all combinations of edges to execute in all threads
    List<Map<MPORThread, CFAEdge>> edgeCombinations = new ArrayList<>();
    cartesianProduct(
        threadEdges, new ArrayList<>(threadEdges.keySet()), 0, new HashMap<>(), edgeCombinations);

    for (Map<MPORThread, CFAEdge> combination : edgeCombinations) {
      MPORState newState = pGapState;
      for (var entry : combination.entrySet()) {
        newState = stateBuilder.createNewState(newState, entry.getKey(), entry.getValue());
      }
      nextStates.add(newState);
    }
    return nextStates.build();
  }

  /**
   * Recursively creates the cartesian product of edge tuples with the number of threads as its
   * size.
   *
   * @param pEdgesByThread the map of threads to their sets of current nodes leaving edges
   * @param pThreads the list of threads we pIndex
   * @param pIndex the current pIndex
   * @param pCurrentCombination the current combination which is created
   * @param pEdgeCombinations the list of combinations (maps form threads to executed edges)
   */
  private void cartesianProduct(
      Map<MPORThread, List<CFAEdge>> pEdgesByThread,
      List<MPORThread> pThreads,
      int pIndex,
      Map<MPORThread, CFAEdge> pCurrentCombination,
      List<Map<MPORThread, CFAEdge>> pEdgeCombinations) {

    if (pIndex == pThreads.size()) {
      pEdgeCombinations.add(new HashMap<>(pCurrentCombination));
      return;
    }
    MPORThread thread = pThreads.get(pIndex);
    List<CFAEdge> edges = pEdgesByThread.get(thread);
    assert edges != null;
    for (CFAEdge edge : edges) {
      pCurrentCombination.put(thread, edge);
      cartesianProduct(
          pEdgesByThread, pThreads, pIndex + 1, pCurrentCombination, pEdgeCombinations);
      pCurrentCombination.remove(thread); // backtrack
    }
  }

  private final ConfigurableProgramAnalysis CPA;

  private final LogManager LOG_MANAGER;

  private final Configuration CONFIG;

  private final ShutdownNotifier SHUTDOWN_NOTIFIER;

  private final Specification SPECIFICATION;

  private final CFA INPUT_CFA;

  private final GlobalAccessChecker GAC;

  private final PredicateTransferRelation PTR;

  private final Sequentialization SEQ;

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

  private final StateBuilder stateBuilder;

  private final ThreadBuilder threadBuilder;

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
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(CPA, PredicateCPA.class, PredicateRefiner.class);
    PTR = predicateCpa.getTransferRelation();
    SEQ =
        new Sequentialization(
            CONFIG, LOG_MANAGER, INPUT_CFA, INPUT_CFA.getMainFunction().getFunction());

    functionCallMap = getFunctionCallMap(INPUT_CFA);
    threadBuilder = new ThreadBuilder(functionCallMap);
    stateBuilder = new StateBuilder(PTR, functionCallMap);

    threads = getThreads(INPUT_CFA, functionCallMap);
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
    assert threadBuilder != null;
    rThreads.add(threadBuilder.createThread(Optional.empty(), mainEntryNode, mainExitNode));

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
        rThreads.add(
            threadBuilder.createThread(Optional.ofNullable(pthreadT), entryNode, exitNode));
      }
    }
    return rThreads.build();
  }

  // (Private) Helpers ===========================================================================

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
      Optional<CFANode> funcReturnNode = pState.funcReturnNodes.get(currentThread);
      ImmutableSet<CFAEdge> returnEdges = MPORUtil.returnLeavingEdges(currentNode, funcReturnNode);
      if (!GAC.anyGlobalAccess(returnEdges)) {
        return false;
      }
    }
    return true;
  }

  private boolean allLeavingEdgesCommute(MPORState pGapState)
      throws CPATransferException, InterruptedException {

    // go through all thread-node pairings
    for (var entryA : pGapState.threadNodes.entrySet()) {
      MPORThread threadA = entryA.getKey();
      for (var entryB : pGapState.threadNodes.entrySet()) {
        MPORThread threadB = entryB.getKey();
        if (threadA != threadB) {
          CFANode nodeA = entryA.getValue();
          CFANode nodeB = entryB.getValue();
          Optional<CFANode> funcReturnNodeA = pGapState.funcReturnNodes.get(threadA);
          Optional<CFANode> funcReturnNodeB = pGapState.funcReturnNodes.get(threadA);
          // go through all pairings of global access leaving edges
          for (CFAEdge edgeA : MPORUtil.returnLeavingEdges(nodeA, funcReturnNodeA)) {
            if (GAC.hasGlobalAccess(edgeA)) {
              for (CFAEdge edgeB : MPORUtil.returnLeavingEdges(nodeB, funcReturnNodeB)) {
                if (GAC.hasGlobalAccess(edgeB)) {
                  if (!MPORUtil.doEdgesCommute(PTR, pGapState.abstractState, edgeA, edgeB)) {
                    return false;
                  }
                }
              }
            }
          }
        }
      }
    }
    return true;
  }
}
