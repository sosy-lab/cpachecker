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
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution.CVariableDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution.SubstituteBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.ExecutionTrace;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.MPORState;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.StateBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order.TSO;
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
   * in (positional) {@link TSO}s and local vs. global accesses.
   *
   * @param pState the current MPORState we analyze
   */
  private void handleState(MPORState pState) throws CPATransferException, InterruptedException {
    // make sure the MPORState was not yet visited to prevent infinite loops
    if (MPORUtil.shouldVisit(stateBuilder.getExistingStates(), pState)) {
      handleTSOs(pState);

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
          for (Map<MPORThread, CFAEdge> combination : getEdgeInterleavingCombinations(gapState)) {
            // TODO for each edge in the combination, create a new state. give these states to the
            //  Interleaving and use them in the goto statements
            //  Interleaving interleaving = new Interleaving(ImmutableMap.copyOf(combination));
          }
        }
      }
    }
  }

  /**
   * Handles the {@link TSO}s of pState by choosing a priority thread which does not wait for any
   * other thread to execute an edge.
   *
   * <p>As a byproduct, this function can detect deadlocks if the tsoGraph contains cycles. However,
   * the absence of deadlocks in this algorithm does not guarantee that the input program is
   * deadlock-free.
   *
   * @throws IllegalArgumentException if a deadlock is found (do not catch)
   */
  private void handleTSOs(MPORState pState) throws CPATransferException, InterruptedException {

    // if there are no TSOs, continue from the input state
    if (pState.totalStrictOrders.isEmpty()) {
      return;
    }

    // otherwise, create a directed graph from subsequent to preceding threads
    DirectedGraph<MPORThread> tsoGraph = new DirectedGraph<>();
    for (TSO tso : pState.totalStrictOrders) {
      MPORThread subsequent = tso.subsequentThread;
      if (!tsoGraph.hasNode(subsequent)) {
        tsoGraph.addNode(subsequent);
      }
      tsoGraph.addEdge(subsequent, tso.precedingThread);
    }
    // search graph for cycles (= deadlocks). if false, the absence of deadlocks is not guaranteed
    if (tsoGraph.containsCycle()) {
      throw new IllegalArgumentException("deadlock detected in state " + pState);
    }

    // retrieve the priority thread which is not waiting on any other thread
    ImmutableSet<MPORThread> maximalScc = tsoGraph.computeSCCs().iterator().next();
    assert maximalScc.size() == 1; // TODO should always hold because there are no loops
    MPORThread priorityThread = maximalScc.asList().get(0);
    // retrieve the priority edges executed by the priority thread and recursively execute them
    ImmutableSet.Builder<CFAEdge> priorityEdges = ImmutableSet.builder();
    for (TSO tso : pState.totalStrictOrders) {
      if (tso.precedingThread.equals(priorityThread)) {
        priorityEdges.addAll(tso.precedingEdges);
      }
    }
    ImmutableSet<CFAEdge> edgesToExecute = priorityEdges.build();

    // execute the priorityThread and create new states to explore
    CFANode priorityNode = pState.threadNodes.get(priorityThread);
    Optional<CFANode> funcReturnNode = pState.funcReturnNodes.get(priorityThread);
    for (CFAEdge edge : MPORUtil.returnLeavingEdges(priorityNode, funcReturnNode)) {
      if (edgesToExecute.contains(edge)) {
        // handle the PO step by step as every new state can induce new POs from another thread
        handleState(stateBuilder.createNewState(pState, priorityThread, edge));
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
        if (!currentNode.equals(currentThread.cfa.exitNode)) {
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

  // TODO
  /*private void interleaveThreads(MPORState pGapState) {
    ImmutableMap.Builder<MPORThread, ImmutableSet<CFAEdge>> threadEdges = ImmutableMap.builder();
    for (var entry : pGapState.threadNodes.entrySet()) {
      MPORThread thread = entry.getKey();
      Optional<CFANode> funcReturnNode = pGapState.funcReturnNodes.get(thread);
      threadEdges.put(thread, MPORUtil.returnLeavingEdges(entry.getValue(), funcReturnNode));
    }
    ImmutableMap<MPORThread, ImmutableSet<CFAEdge>> unused = threadEdges.buildOrThrow();
  }*/

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

    // TODO use getEdgeInterleavingCombinations here
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

  private List<Map<MPORThread, CFAEdge>> getEdgeInterleavingCombinations(MPORState pGapState) {
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
    List<Map<MPORThread, CFAEdge>> rEdgeCombinations = new ArrayList<>();
    cartesianProduct(
        threadEdges, new ArrayList<>(threadEdges.keySet()), 0, new HashMap<>(), rEdgeCombinations);

    return rEdgeCombinations;
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
  private final ImmutableMap<CFANode, CFANode> funcCallMap;

  private final ImmutableMap<CFunctionType, ImmutableSet<CReturnStatementEdge>> funcReturnEdges;

  private final StateBuilder stateBuilder;

  private final ThreadBuilder threadBuilder;

  private final CBinaryExpressionBuilder cBinExprBuilder;

  /** The set of global variable declarations in the input program, used to identify variables. */
  private final ImmutableSet<CVariableDeclaration> globalVars;

  private final CVariableDeclarationSubstitution cVarDecSubstitution;

  private final ImmutableMap<CVariableDeclaration, CVariableDeclaration> varSubstitutes;

  private final ImmutableMap<CFAEdge, CFAEdge> edgeSubstitutes;

  /**
   * The set of threads in the program, including the main thread and all pthreads. Needs to be
   * initialized after {@link MPORAlgorithm#funcCallMap}.
   */
  private final ImmutableSet<MPORThread> threads;

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

    funcCallMap = getFunctionCallMap(INPUT_CFA);
    funcReturnEdges = getFuncReturnEdges(INPUT_CFA);

    threadBuilder = new ThreadBuilder(funcCallMap);
    stateBuilder = new StateBuilder(PTR, funcCallMap);
    cBinExprBuilder = new CBinaryExpressionBuilder(INPUT_CFA.getMachineModel(), pLogManager);

    globalVars = getGlobalVars(INPUT_CFA);
    threads = getThreads(INPUT_CFA, funcCallMap);

    varSubstitutes = getVarSubstitutes();
    cVarDecSubstitution = new CVariableDeclarationSubstitution(varSubstitutes, cBinExprBuilder);
    edgeSubstitutes = getEdgeSubstitutes();
    threadBuilder.initEdgeSubstitutes(edgeSubstitutes, threads);

    SEQ = new Sequentialization(threads.size());
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
      if (PthreadFuncType.isCallToPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
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

    FunctionEntryNode mainFunctionEntryNode = getMainThread(pThreads).cfa.entryNode;
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

  private ImmutableMap<CFunctionType, ImmutableSet<CReturnStatementEdge>> getFuncReturnEdges(
      CFA pCfa) {
    ImmutableMap.Builder<CFunctionType, ImmutableSet<CReturnStatementEdge>> rFuncReturnEdges =
        ImmutableMap.builder();
    for (FunctionEntryNode entryNode : pCfa.entryNodes()) {
      CFunctionType entryCFuncType = MPORUtil.getCFuncTypeFromCfaNode(entryNode);
      ImmutableSet.Builder<CReturnStatementEdge> returnEdges = ImmutableSet.builder();
      for (CFAEdge edge : pCfa.edges()) {
        if (edge instanceof CReturnStatementEdge cFuncReturnEdge) {
          CFunctionType cFuncType =
              MPORUtil.getCFuncTypeFromCfaNode(cFuncReturnEdge.getPredecessor());
          if (cFuncType.equals(entryCFuncType)) {
            returnEdges.add(cFuncReturnEdge);
          }
        }
      }
      rFuncReturnEdges.put(entryCFuncType, returnEdges.build());
    }
    return rFuncReturnEdges.buildOrThrow();
  }

  /** Extracts all global variable declarations from pCfa. */
  private ImmutableSet<CVariableDeclaration> getGlobalVars(CFA pCfa) {
    ImmutableSet.Builder<CVariableDeclaration> rGlobalVars = ImmutableSet.builder();
    for (CFAEdge edge : pCfa.edges()) {
      if (edge instanceof CDeclarationEdge declarationEdge) {
        if (GAC.hasGlobalAccess(edge) && declarationEdge.getDeclaration().isGlobal()) {
          AAstNode aAstNode = declarationEdge.getRawAST().orElseThrow();
          // exclude FunctionDeclarations
          if (aAstNode instanceof CVariableDeclaration cVariableDeclaration) {
            rGlobalVars.add(cVariableDeclaration);
          }
        }
      }
    }
    return rGlobalVars.build();
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
      if (PthreadFuncType.isCallToPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
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

  /**
   * Creates substitutes for all variables in the program and maps them to their original. The
   * substitutes differ only in their name.
   */
  private ImmutableMap<CVariableDeclaration, CVariableDeclaration> getVarSubstitutes() {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap.Builder<CVariableDeclaration, CVariableDeclaration> dummySubBuilder =
        ImmutableMap.builder();
    for (CVariableDeclaration globalVar : globalVars) {
      String substituteName = SubstituteBuilder.substituteGlobalVarName(globalVar);
      CVariableDeclaration substitution =
          SubstituteBuilder.substituteVarDec(globalVar, substituteName);
      dummySubBuilder.put(globalVar, substitution);
    }
    for (MPORThread thread : threads) {
      for (CVariableDeclaration localVar : thread.localVars) {
        String substituteName = SubstituteBuilder.substituteLocalVarName(localVar, thread.id);
        CVariableDeclaration substitute =
            SubstituteBuilder.substituteVarDec(localVar, substituteName);
        dummySubBuilder.put(localVar, substitute);
      }
    }
    ImmutableMap<CVariableDeclaration, CVariableDeclaration> dummySubs =
        dummySubBuilder.buildOrThrow();

    // create temporary local CVarDecSubstitution
    CVariableDeclarationSubstitution cVarDecSub =
        new CVariableDeclarationSubstitution(dummySubs, cBinExprBuilder);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableMap.Builder<CVariableDeclaration, CVariableDeclaration> rFinalSubs =
        ImmutableMap.builder();
    // TODO handle CInitializerList
    for (var entry : dummySubs.entrySet()) {
      CInitializer cInitializer = entry.getValue().getInitializer();
      if (cInitializer != null) {
        if (cInitializer instanceof CInitializerExpression cInitExpr) {
          CInitializerExpression cInitExprSub =
              SubstituteBuilder.substituteInitExpr(
                  cInitExpr, cVarDecSub.substitute(cInitExpr.getExpression()));
          CVariableDeclaration finalSub =
              SubstituteBuilder.substituteVarDec(entry.getValue(), cInitExprSub);
          rFinalSubs.put(entry.getKey(), finalSub);
          continue;
        }
      }
      rFinalSubs.put(entry);
    }
    return rFinalSubs.buildOrThrow();
  }

  private ImmutableMap<CFAEdge, CFAEdge> getEdgeSubstitutes() {
    Map<CFAEdge, CFAEdge> rSubstitutes = new HashMap<>();
    for (CFAEdge edge : CFAUtils.allEdges(INPUT_CFA)) {
      // prevent duplicate keys by excluding parallel edges
      if (!rSubstitutes.containsKey(edge)) {
        CFAEdge substitute = null;

        if (edge instanceof CDeclarationEdge cDecEdge) {
          // TODO what about structs?
          CDeclaration cDec = cDecEdge.getDeclaration();
          if (cDec instanceof CVariableDeclaration cVarDec) {
            if (varSubstitutes.containsKey(cVarDec)) {
              substitute =
                  SubstituteBuilder.substituteDeclarationEdge(
                      cDecEdge, varSubstitutes.get(cVarDec));
            }
          }

        } else if (edge instanceof CAssumeEdge cAssumeEdge) {
          substitute =
              SubstituteBuilder.substituteAssumeEdge(
                  cAssumeEdge, cVarDecSubstitution.substitute(cAssumeEdge.getExpression()));

        } else if (edge instanceof CStatementEdge cStmtEdge) {
          substitute =
              SubstituteBuilder.substituteStatementEdge(
                  cStmtEdge, cVarDecSubstitution.substitute(cStmtEdge.getStatement()));
        }
        rSubstitutes.put(edge, substitute == null ? edge : substitute);
      }
    }
    return ImmutableMap.copyOf(rSubstitutes);
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
