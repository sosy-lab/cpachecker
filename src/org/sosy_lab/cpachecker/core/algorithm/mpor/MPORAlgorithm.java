// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORJoin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.PreferenceOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.tests.MPORTests;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;

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
 *       undefined (TODO use loop structures?)
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
    checkForCorrectInitialState(pReachedSet, threads);
    // if there is only one element in pReachedSet, it is our initial AbstractState
    AbstractState initialAbstractState = pReachedSet.asCollection().iterator().next();
    MPORState initialState = getInitialState(threads, initialAbstractState);
    HandleState(new HashSet<>(), initialState, new HashSet<>());
    /*

    overall algorithm idea:

       (1) for each AbstractState in ReachedSet:

           create MPORState based on AbstractState and map the two
           create MPORState PathFormula (= all visited CFAEdges so far, get newPathFormula with
           convertEdgeToPathFormula(abstractState.getPathFormula(), CFAEdge)), the set of
           PreferenceOrders and ConflictRelations

           for each Node in MPORState
               for each context sensitive leaving Edge of Node excluding edges that can be pruned
               (see Algorithm 2 CompatiblePersistentSet(state))
               use GlobalAccessChecker to check whether a CfaEdge reads or writes global
               / shared variables. only shared variable access Edges are relevant for the algorithm
                    create new set of AbstractStates when executing Edge, see
                    PredicateTransferRelation.getAbstractSuccessorsForEdge(AbstractState, Edge),
                    (repeat from (1) until all threads have terminated, i.e. all are at their
                    exitNodes). its best to create a separate function HandleAbstractState(...)


        (2) "a commutes with b under phi":

            from MPORState / AbstractState q
            for each possible combination (A, B) of two context-sensitive leaving edges that cannot
            be pruned

                create AbstractStates for a = [currentPath then A] and b = [currentPath then B]
                create PathFormulas for abPath = [a then B] and baPath = [b then A]
                check if NOT unsatcheck(a, abPath) and NOT unsatCheck(b, baPath) holds
                if one holds, "a commutes with b under phi" is not fulfilled
    */

    // TODO
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /**
   * Recursively checks the reached MPORStates.
   *
   * @param pVisitedStates the set of already visited MPORStates
   * @param pState the current MPORState we analyze
   * @param pSleepSet the set of CFAEdges that can safely be skipped
   */
  private void HandleState(
      Set<MPORState> pVisitedStates, MPORState pState, Set<CFAEdge> pSleepSet) {
    // TODO
  }

  private final ConfigurableProgramAnalysis cpa;

  private final LogManager logger;

  private final Configuration config;

  private final ShutdownNotifier shutdownNotifier;

  private final Specification specification;

  private final CFA cfa;

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

  // TODO use CFAToCTranslator translateCfa to generate a C program based on a CFA
  //  this will be used for the reduced and sequentialized CFA

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

    // TODO performance stuff:
    //  merge functions that go through each Edge together into one?
    //  merge functions that go through each Node together into one?

    checkForCProgram(cfa);
    checkForParallelProgram(cfa);

    functionCallMap = getFunctionCallMap(cfa);
    threads = getThreads(cfa);

    assignMutexesToThreads(threads);
    assignJoinsToThreads(threads);
    // TODO assignBarriersToThreads(threads);

    MPORTests test = new MPORTests(this);
    test.computeAllStates();
  }

  /** Checks whether the input language of the program is C and throws an exception if not. */
  private void checkForCProgram(CFA pCfa) {
    checkArgument(
        pCfa.getMetadata().getInputLanguage().equals(Language.C), "MPOR expects C program");
  }

  /**
   * Checks whether any edge in the CFA contains a pthread_create call. If that is not the case, the
   * algorithm ends and the user is informed that MPOR is meant to analyze parallel programs.
   */
  private void checkForParallelProgram(CFA pCfa) {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_CREATE)) {
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
  private ImmutableMap<CFANode, CFANode> getFunctionCallMap(CFA pCfa) {
    ImmutableMap.Builder<CFANode, CFANode> rFunctionCallMap = ImmutableMap.builder();
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (cfaEdge instanceof FunctionCallEdge functionCallEdge) {
        rFunctionCallMap.put(functionCallEdge.getPredecessor(), functionCallEdge.getReturnNode());
      }
    }
    return rFunctionCallMap.buildOrThrow();
  }

  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * <p>This functions needs to be called after functionCallMap was initialized so that we can track
   * the calling context of each thread.
   *
   * @param pCfa the CFA to be analyzed
   * @return the set of threads
   */
  private ImmutableSet<MPORThread> getThreads(CFA pCfa) {
    ImmutableSet.Builder<MPORThread> rThreads = ImmutableSet.builder();

    // add the main thread
    FunctionEntryNode mainEntryNode = pCfa.getMainFunction();
    FunctionExitNode mainExitNode = getFunctionExitNode(mainEntryNode);
    rThreads.add(createThread(Optional.empty(), mainEntryNode, mainExitNode));

    // search the CFA for pthread_create calls
    for (CFAEdge cfaEdge : CFAUtils.allUniqueEdges(pCfa)) {
      if (isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_CREATE)) {
        // extract the first parameter of pthread_create, i.e. the pthread_t value
        CExpression pthreadT =
            CFAUtils.getValueFromPointer(CFAUtils.getParameterAtIndex(cfaEdge, 0));
        // extract the third parameter of pthread_create which points to the start routine function
        CFunctionType startRoutine =
            CFAUtils.getCFunctionTypeFromCExpression(CFAUtils.getParameterAtIndex(cfaEdge, 2));
        FunctionEntryNode entryNode =
            CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, startRoutine);
        FunctionExitNode exitNode = getFunctionExitNode(entryNode);
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
    Set<CFANode> threadNodes = new HashSet<>();
    ImmutableSet.Builder<CFAEdge> threadEdges = ImmutableSet.builder();
    initThreadVariables(pExitNode, threadNodes, threadEdges, pEntryNode, null);
    return new MPORThread(
        pPthreadT, pEntryNode, pExitNode, ImmutableSet.copyOf(threadNodes), threadEdges.build());
  }

  /**
   * Searches the CFA of a thread specified by its entry node (the first pCurrentNode) and
   * pExitNode.
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
              getFunctionReturnNode(pCurrentNode, pFunctionReturnNode, functionCallMap));
        }
      }
    }
  }

  /**
   * Searches all threads in pThreads for mutex locks and properly initializes the corresponding
   * MPORMutex objects.
   *
   * @param pThreads the set of threads whose main functions / start routines are searched
   */
  private void assignMutexesToThreads(ImmutableSet<MPORThread> pThreads) {
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

    if (pVisitedNodes.contains(pCurrentNode) || pCurrentNode.equals(pThread.exitNode)) {
      return;
    }
    for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
      if (isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_MUTEX_LOCK)) {
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
        pThread.addMutex(mutex);
      }
      pVisitedNodes.add(pCurrentNode);
      searchThreadForMutexes(
          pThread,
          pVisitedNodes,
          cfaEdge.getSuccessor(),
          getFunctionReturnNode(pCurrentNode, pFunctionReturnNode, functionCallMap));
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
      final Set<CFANode> pMutexNodes, // using a set so that we can use .contains(...)
      final ImmutableSet.Builder<CFAEdge> pMutexEdges,
      final ImmutableSet.Builder<CFANode> pMutexExitNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    // visit CFANodes only once to prevent infinite loops in case of loop structures
    if (!pMutexNodes.contains(pCurrentNode)) {
      pMutexNodes.add(pCurrentNode);
      for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
        pMutexEdges.add(cfaEdge);
        if (isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_MUTEX_UNLOCK)) {
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
  private void assignJoinsToThreads(ImmutableSet<MPORThread> pThreads) {
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
      ImmutableSet<MPORThread> pThreads,
      MPORThread pThread,
      Set<CFANode> pVisitedNodes,
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode) {

    if (pVisitedNodes.contains(pCurrentNode) || pCurrentNode.equals(pThread.exitNode)) {
      return;
    }
    for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
      if (isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_JOIN)) {
        CExpression pthreadT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
        MPORThread threadToTerminate = getThreadByPthreadT(pThreads, pthreadT);
        MPORJoin join = new MPORJoin(threadToTerminate, pCurrentNode, cfaEdge);
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
    if (pVisitedNodes.contains(pCurrentNode) || pCurrentNode.equals(pThread.exitNode)) {
      return;
    }
    for (CFAEdge cfaEdge : contextSensitiveLeavingEdges(pCurrentNode, pFunctionReturnNode)) {
      if (isEdgeCallToFunctionType(cfaEdge, FunctionType.BARRIER_INIT)) {
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
  }

  /**
   * Computes and returns the PreferenceOrders for the current program state pState.
   *
   * @param pThreadNodes the threads and their current CFANodes to be analyzed
   * @return an ImmutableSet of (positional) PreferenceOrders for the given threadNodes
   */
  public static ImmutableSet<PreferenceOrder> getPreferenceOrdersForThreadNodes(
      ImmutableMap<MPORThread, CFANode> pThreadNodes) {
    ImmutableSet.Builder<PreferenceOrder> rPreferenceOrders = ImmutableSet.builder();
    for (var entry : pThreadNodes.entrySet()) {
      MPORThread currentThread = entry.getKey();
      CFANode currentNode = entry.getValue();
      // TODO getCreationPreferenceOrders (e.g. main thread: all edges before the first pthread
      //  creation are precedingEdges to the subsequentEdge pthread_create
      rPreferenceOrders.addAll(getMutexPreferenceOrders(pThreadNodes, currentThread, currentNode));
      rPreferenceOrders.addAll(getJoinPreferenceOrders(pThreadNodes, currentThread, currentNode));
    }
    return rPreferenceOrders.build();
  }

  /**
   * Computes and returns the PreferenceOrders induced by mutex locks in the program.
   *
   * @param pThreadNodes the threads and their current CFANodes
   * @param pCurrentThread the thread where we check if it is inside a mutex lock
   * @param pCurrentNode the current CFANode of pCurrentThread
   * @return the set of PreferenceOrders induced by mutex locks
   */
  private static ImmutableSet<PreferenceOrder> getMutexPreferenceOrders(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      MPORThread pCurrentThread,
      CFANode pCurrentNode) {

    ImmutableSet.Builder<PreferenceOrder> rMutexPreferenceOrders = ImmutableSet.builder();

    // if pCurrentThread is in a mutex lock
    for (MPORMutex mutex : pCurrentThread.getMutexes()) {
      if (mutex.cfaNodes.contains(pCurrentNode)) {

        // search all other threads for pthread_mutex_lock calls to the same pthread_mutex_t object
        for (var entry : pThreadNodes.entrySet()) {
          if (!entry.getKey().equals(pCurrentThread)) {
            CFANode otherNode = entry.getValue();
            for (CFAEdge cfaEdge : CFAUtils.leavingEdges(otherNode)) {
              if (isEdgeCallToFunctionType(cfaEdge, FunctionType.PTHREAD_MUTEX_LOCK)) {
                CExpression pthreadMutexT = CFAUtils.getParameterAtIndex(cfaEdge, 0);
                if (pthreadMutexT.equals(mutex.pthreadMutexT)) {

                  // extract all CFAEdges inside mutex excluding the leaving edges of exitNodes
                  ImmutableSet.Builder<CFAEdge> precedingEdges = ImmutableSet.builder();
                  precedingEdges.addAll(mutex.cfaEdges);
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
  private static ImmutableSet<PreferenceOrder> getJoinPreferenceOrders(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      MPORThread pCurrentThread,
      CFANode pCurrentNode) {

    ImmutableSet.Builder<PreferenceOrder> rJoinPreferenceOrders = ImmutableSet.builder();

    // if pCurrentThread is right before a pthread_join call
    for (MPORJoin join : pCurrentThread.getJoins()) {
      if (pCurrentNode.equals(join.preJoinNode)) {
        CExpression pthreadT = CFAUtils.getParameterAtIndex(join.joinEdge, 0);
        MPORThread threadToTerminate = getThreadByPthreadT(pThreadNodes, pthreadT);
        // if the thread specified as pthread_t in the pthread_join call has not yet terminated
        CFANode threadToTerminateCurrentNode = pThreadNodes.get(threadToTerminate);
        assert threadToTerminateCurrentNode != null;
        if (!threadToTerminateCurrentNode.equals(threadToTerminate.exitNode)) {
          // add all CFAEdges executed by pthread_t as preceding edges
          ImmutableSet<CFAEdge> precedingEdges = threadToTerminate.cfaEdges;
          rJoinPreferenceOrders.add(new PreferenceOrder(precedingEdges, join.joinEdge));
        }
      }
    }
    return rJoinPreferenceOrders.build();
  }

  // Helpers =======================================================================================

  /**
   * Returns the initial MPORState of the program, properly initializing the map from MPORThreads to
   * their start routines / main FunctionEntryNodes, the PreferenceOrders and the corresponding
   * AbstractState.
   *
   * @param pThreads the set of Threads we put in {@link MPORState#threadNodes}
   * @param initialAbstractState the initial AbstractState in {@link MPORAlgorithm#run}
   * @return the initial MPORState of the program
   */
  private MPORState getInitialState(
      ImmutableSet<MPORThread> pThreads, AbstractState initialAbstractState) {
    ImmutableMap.Builder<MPORThread, CFANode> threadNodesBuilder = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      threadNodesBuilder.put(thread, thread.entryNode);
    }
    ImmutableMap<MPORThread, CFANode> threadNodes = threadNodesBuilder.buildOrThrow();
    return new MPORState(
        threadNodes, getPreferenceOrdersForThreadNodes(threadNodes), initialAbstractState);
  }

  /**
   * Tries to extract the FunctionExitNode from the given FunctionEntryNode of a threads start
   * routine / main function.
   *
   * @param pStartRoutineEntryNode the FunctionEntryNode of the start routine / main function of a
   *     thread
   * @return the FunctionExitNode of FunctionEntryNode if it is present
   * @throws IllegalArgumentException if the exit node is empty (see {@link MPORAlgorithm}
   *     restrictions)
   */
  public static FunctionExitNode getFunctionExitNode(FunctionEntryNode pStartRoutineEntryNode) {
    try {
      return pStartRoutineEntryNode.getExitNode().orElseThrow();
    } catch (Exception e) {
      // using a custom exception here for more info
      throw new IllegalArgumentException(
          "pFunctionExitNode is empty, all start routines / main functions must contain an exit"
              + " node in MPOR");
    }
  }

  /**
   * Searches the given Set of MPORThreads for the given pPthreadT object.
   *
   * @param pThreads the set of MPORThreads to be searched
   * @param pPthreadT the pthread_t object as a CExpression
   * @return the MPORThread object with pPthreadT as its threadObject (pthread_t)
   * @throws IllegalArgumentException if no thread exists in the set whose threadObject is pPthreadT
   */
  public static MPORThread getThreadByPthreadT(
      ImmutableSet<MPORThread> pThreads, CExpression pPthreadT) {
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
   * Searches the given Set of MPORThreads for the given pPthreadT object.
   *
   * @param pThreadNodes the map of MPORThreads to their current CFANodes to be searched
   * @param pPthreadT the pthread_t object as a CExpression
   * @return the MPORThread object with pPthreadT as its threadObject (pthread_t)
   * @throws IllegalArgumentException if no thread exists in the map whose threadObject is pPthreadT
   */
  public static MPORThread getThreadByPthreadT(
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
  public static MPORThread getMainThread(ImmutableSet<MPORThread> pThreads) {
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
   * @param pCurrentNode the current CFANode to be analyzed
   * @param pFunctionReturnNode the previous pFunctionReturnNode
   * @param pFunctionCallMap map from CFANodes before FunctionCallEdges to FunctionReturnNodes
   * @return the previous or new FunctionReturnNode or null if pCurrentNode exits a function
   */
  public static CFANode getFunctionReturnNode(
      CFANode pCurrentNode,
      CFANode pFunctionReturnNode,
      ImmutableMap<CFANode, CFANode> pFunctionCallMap) {
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
    if (pCurrentNode instanceof FunctionExitNode) {
      return CFAUtils.leavingEdges(pCurrentNode)
          .filter(cfaEdge -> cfaEdge.getSuccessor().equals(pFunctionReturnNode));
    } else {
      return CFAUtils.leavingEdges(pCurrentNode);
    }
  }

  /**
   * Tries to extract the CFunctionCallStatement from pCfaEdge and checks if it is a call to
   * pFunctionType.
   *
   * @param pCfaEdge the CFAEdge to be analyzed
   * @param pFunctionType the desired FunctionType
   * @return true if pCfaEdge is a call to pFunctionType
   */
  public static boolean isEdgeCallToFunctionType(CFAEdge pCfaEdge, FunctionType pFunctionType) {
    return CFAUtils.isCfaEdgeCFunctionCallStatement(pCfaEdge)
        && CFAUtils.getCFunctionCallStatementFromCfaEdge(pCfaEdge)
            .getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString()
            .equals(pFunctionType.name);
  }

  /**
   * Returns a new state with the same threadNodes map except that the key pThread is assigned the
   * new value pUpdatedNode. This function also computes the PreferenceOrders of the new state.
   *
   * @param pThread The MPORThread that has a new CFANode (= state)
   * @param pUpdatedNode The updated CFANode (= state) of pThread
   * @return a new MPORState with the updated value pUpdatedNode at key pThread and the
   *     corresponding PreferenceOrders
   */
  public MPORState createUpdatedState(
      MPORState pState, MPORThread pThread, CFANode pUpdatedNode, AbstractState pAbstractState) {
    checkArgument(pState.threadNodes.containsKey(pThread), "threadNodes must contain pThread");
    ImmutableMap.Builder<MPORThread, CFANode> threadNodesBuilder = ImmutableMap.builder();
    for (var entry : pState.threadNodes.entrySet()) {
      if (!entry.getKey().equals(pThread)) {
        threadNodesBuilder.put(entry);
      }
    }
    threadNodesBuilder.put(pThread, pUpdatedNode);
    ImmutableMap<MPORThread, CFANode> updatedThreadNodes = threadNodesBuilder.buildOrThrow();
    return new MPORState(
        updatedThreadNodes, getPreferenceOrdersForThreadNodes(updatedThreadNodes), pAbstractState);
  }

  // TODO these can be deleted later
  // Test =======================================================================================

  public ImmutableSet<MPORThread> getThreads() {
    return threads;
  }

  public ImmutableMap<CFANode, CFANode> getFunctionCallMap() {
    return functionCallMap;
  }
}
