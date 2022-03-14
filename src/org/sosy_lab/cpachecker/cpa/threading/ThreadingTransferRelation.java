// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import static com.google.common.collect.Collections2.transform;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.CFACloner;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

@Options(prefix = "cpa.threading")
public final class ThreadingTransferRelation extends SingleEdgeTransferRelation {

  @Option(
      description =
          "do not use the original functions from the CFA, but cloned ones. "
              + "See cfa.postprocessing.CFACloner for detail.",
      secure = true)
  private boolean useClonedFunctions = true;

  @Option(
      description =
          "allow assignments of a new thread to the same left-hand-side as an existing thread.",
      secure = true)
  private boolean allowMultipleLHS = false;

  @Option(
      description =
          "the maximal number of parallel threads, -1 for infinite. When combined with"
              + " 'useClonedFunctions=true', we need at least N cloned functions. The option"
              + " 'cfa.cfaCloner.numberOfCopies' should be set to N.",
      secure = true)
  private int maxNumberOfThreads = 5;

  @Option(
      description =
          "atomic locks are used to simulate atomic statements, as described in the rules of"
              + " SV-Comp.",
      secure = true)
  private boolean useAtomicLocks = true;

  @Option(
      description =
          "local access locks are used to avoid expensive interleaving, "
              + "if a thread only reads and writes its own variables.",
      secure = true)
  private boolean useLocalAccessLocks = true;

  @Option(
      description =
          "in case of witness validation we need to check all possible function calls of cloned"
              + " CFAs.",
      secure = true)
  private boolean useAllPossibleClones = false;

  public static final String THREAD_START = "pthread_create";
  public static final String THREAD_JOIN = "pthread_join";
  private static final String THREAD_EXIT = "pthread_exit";
  private static final String THREAD_MUTEX_LOCK = "pthread_mutex_lock";
  private static final String THREAD_MUTEX_UNLOCK = "pthread_mutex_unlock";
  private static final String VERIFIER_ATOMIC = "__VERIFIER_atomic_";
  private static final String VERIFIER_ATOMIC_BEGIN = "__VERIFIER_atomic_begin";
  private static final String VERIFIER_ATOMIC_END = "__VERIFIER_atomic_end";
  private static final String ATOMIC_LOCK = "__CPAchecker_atomic_lock__";
  private static final String LOCAL_ACCESS_LOCK = "__CPAchecker_local_access_lock__";
  private static final String THREAD_ID_SEPARATOR = "__CPAchecker__";

  private static final ImmutableSet<String> UNSUPPORTED_THREAD_FUNCTIONS =
      ImmutableSet.of(
          // https://gitlab.com/sosy-lab/software/cpachecker/-/issues/929
          "pthread_cond_wait", "pthread_cond_timedwait");

  private static final ImmutableSet<String> THREAD_FUNCTIONS =
      ImmutableSet.of(
          THREAD_START,
          THREAD_MUTEX_LOCK,
          THREAD_MUTEX_UNLOCK,
          THREAD_JOIN,
          THREAD_EXIT,
          VERIFIER_ATOMIC_BEGIN,
          VERIFIER_ATOMIC_END);

  private final CFA cfa;
  private final LogManagerWithoutDuplicates logger;
  private final CallstackCPA callstackCPA;
  private final ConfigurableProgramAnalysis locationCPA;

  private final GlobalAccessChecker globalAccessChecker = new GlobalAccessChecker();

  public ThreadingTransferRelation(Configuration pConfig, CFA pCfa, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    locationCPA = LocationCPA.create(pCfa, pConfig);
    callstackCPA = new CallstackCPA(pConfig, pLogger);
    logger = new LogManagerWithoutDuplicates(pLogger);
  }

  @Override
  public Collection<ThreadingState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(cfaEdge);

    ThreadingState state = (ThreadingState) pState;

    ThreadingState threadingState = exitThreads(state);

    final String activeThread = getActiveThread(cfaEdge, threadingState);
    if (null == activeThread) {
      return ImmutableSet.of();
    }

    // check if atomic lock exists and is set for current thread
    if (useAtomicLocks
        && threadingState.hasLock(ATOMIC_LOCK)
        && !threadingState.hasLock(activeThread, ATOMIC_LOCK)) {
      return ImmutableSet.of();
    }

    // check if a local-access-lock allows to avoid exploration of some threads
    if (useLocalAccessLocks) {
      threadingState = handleLocalAccessLock(cfaEdge, threadingState, activeThread);
      if (threadingState == null) {
        return ImmutableSet.of();
      }
    }

    // check, if we can abort the complete analysis of all other threads after this edge.
    if (isEndOfMainFunction(cfaEdge) || isTerminatingEdge(cfaEdge)) {
      // VERIFIER_assume not only terminates the current thread, but the whole program
      return ImmutableSet.of();
    }

    // get all possible successors
    Collection<ThreadingState> results =
        getAbstractSuccessorsFromWrappedCPAs(activeThread, threadingState, precision, cfaEdge);

    results = getAbstractSuccessorsForEdge0(cfaEdge, threadingState, activeThread, results);

    // Store the active thread in the given states, cf. JavaDoc of activeThread
    results = Collections2.transform(results, ts -> ts.withActiveThread(activeThread));

    return ImmutableList.copyOf(results);
  }

  /**
   * Search for the thread, where the current edge is available. The result should be exactly one
   * thread, that is denoted as 'active', or NULL, if no active thread is available.
   *
   * <p>This method is needed, because we use the CompositeCPA to choose the edge, and when we have
   * several locations in the threadingState, only one of them has an outgoing edge matching the
   * current edge.
   */
  @Nullable
  private String getActiveThread(final CFAEdge cfaEdge, final ThreadingState threadingState) {
    final Set<String> activeThreads = new HashSet<>();
    for (String id : threadingState.getThreadIds()) {
      if (Iterables.contains(threadingState.getThreadLocation(id).getOutgoingEdges(), cfaEdge)) {
        activeThreads.add(id);
      }
    }

    assert activeThreads.size() <= 1 : "multiple active threads are not allowed: " + activeThreads;
    // then either the same function is called in different threads -> not supported.
    // (or CompositeCPA and ThreadingCPA do not work together)

    return activeThreads.isEmpty() ? null : Iterables.getOnlyElement(activeThreads);
  }

  /**
   * handle all edges related to thread-management: THREAD_START, THREAD_JOIN, THREAD_EXIT,
   * THREAD_MUTEX_LOCK, VERIFIER_ATOMIC,...
   *
   * <p>If nothing changes, then return <code>results</code> unmodified.
   */
  private Collection<ThreadingState> getAbstractSuccessorsForEdge0(
      final CFAEdge cfaEdge,
      final ThreadingState threadingState,
      final String activeThread,
      final Collection<ThreadingState> results)
      throws UnrecognizedCodeException, InterruptedException {
    switch (cfaEdge.getEdgeType()) {
      case StatementEdge:
        {
          AStatement statement = ((AStatementEdge) cfaEdge).getStatement();
          if (statement instanceof AFunctionCall) {
            AExpression functionNameExp =
                ((AFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
            if (functionNameExp instanceof AIdExpression) {
              final String functionName = ((AIdExpression) functionNameExp).getName();
              if (UNSUPPORTED_THREAD_FUNCTIONS.contains(functionName)) {
                throw new UnsupportedCodeException("pthread condition variables", cfaEdge);
              }
              switch (functionName) {
                case THREAD_START:
                  return startNewThread(threadingState, statement, results, cfaEdge);
                case THREAD_MUTEX_LOCK:
                  return addLock(threadingState, activeThread, extractLockId(statement), results);
                case THREAD_MUTEX_UNLOCK:
                  return removeLock(activeThread, extractLockId(statement), results);
                case THREAD_JOIN:
                  return joinThread(threadingState, statement, results);
                case THREAD_EXIT:
                  // this function-call is already handled in the beginning with isLastNodeOfThread.
                  // return exitThread(threadingState, activeThread, results);
                  break;
                case VERIFIER_ATOMIC_BEGIN:
                  if (useAtomicLocks) {
                    return addLock(threadingState, activeThread, ATOMIC_LOCK, results);
                  }
                  break;
                case VERIFIER_ATOMIC_END:
                  if (useAtomicLocks) {
                    return removeLock(activeThread, ATOMIC_LOCK, results);
                  }
                  break;
                default:
                  // nothing to do, return results
              }
            }
          }
          break;
        }
      case FunctionCallEdge:
        {
          if (useAtomicLocks) {
            // cloning changes the function-name -> we use 'startsWith'.
            // we have 2 different atomic sequences:
            //   1) from calling VERIFIER_ATOMIC_BEGIN to exiting VERIFIER_ATOMIC_END.
            //      (@Deprecated, for old benchmark tasks)
            //   2) from calling VERIFIER_ATOMIC_X to exiting VERIFIER_ATOMIC_X where X can be
            // anything
            final String calledFunction = cfaEdge.getSuccessor().getFunctionName();
            if (calledFunction.startsWith(VERIFIER_ATOMIC_BEGIN)) {
              return addLock(threadingState, activeThread, ATOMIC_LOCK, results);
            } else if (calledFunction.startsWith(VERIFIER_ATOMIC)
                && !calledFunction.startsWith(VERIFIER_ATOMIC_END)) {
              return addLock(threadingState, activeThread, ATOMIC_LOCK, results);
            }
          }
          break;
        }
      case FunctionReturnEdge:
        {
          if (useAtomicLocks) {
            // cloning changes the function-name -> we use 'startsWith'.
            // we have 2 different atomic sequences:
            //   1) from calling VERIFIER_ATOMIC_BEGIN to exiting VERIFIER_ATOMIC_END.
            //      (@Deprecated, for old benchmark tasks)
            //   2) from calling VERIFIER_ATOMIC_X to exiting VERIFIER_ATOMIC_X  where X can be
            // anything
            final String exitedFunction = cfaEdge.getPredecessor().getFunctionName();
            if (exitedFunction.startsWith(VERIFIER_ATOMIC_END)) {
              return removeLock(activeThread, ATOMIC_LOCK, results);
            } else if (exitedFunction.startsWith(VERIFIER_ATOMIC)
                && !exitedFunction.startsWith(VERIFIER_ATOMIC_BEGIN)) {
              return removeLock(activeThread, ATOMIC_LOCK, results);
            }
          }
          break;
        }
      default:
        // nothing to do
    }
    return results;
  }

  /**
   * compute successors for the current edge. There will be only one successor in most cases, even
   * with N threads, because the edge limits the transitions to only one thread, because the
   * LocationTransferRelation will only find one succeeding CFAnode.
   */
  private Collection<ThreadingState> getAbstractSuccessorsFromWrappedCPAs(
      String activeThread, ThreadingState threadingState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    // compute new locations
    Collection<? extends AbstractState> newLocs =
        locationCPA
            .getTransferRelation()
            .getAbstractSuccessorsForEdge(
                threadingState.getThreadLocation(activeThread), precision, cfaEdge);

    // compute new stacks
    Collection<? extends AbstractState> newStacks =
        callstackCPA
            .getTransferRelation()
            .getAbstractSuccessorsForEdge(
                threadingState.getThreadCallstack(activeThread), precision, cfaEdge);

    // combine them pairwise, all combinations needed
    final Collection<ThreadingState> results = new ArrayList<>();
    for (AbstractState loc : newLocs) {
      for (AbstractState stack : newStacks) {
        results.add(threadingState.updateLocationAndCopy(activeThread, stack, loc));
      }
    }

    return results;
  }

  /**
   * checks whether the location is the last node of a thread, i.e. the current thread will
   * terminate after this node.
   */
  public static boolean isLastNodeOfThread(CFANode node) {

    if (0 == node.getNumLeavingEdges()) {
      return true;
    }

    if (1 == node.getNumEnteringEdges()) {
      return isThreadExit(node.getEnteringEdge(0));
    }

    return false;
  }

  private static boolean isThreadExit(CFAEdge cfaEdge) {
    if (CFAEdgeType.StatementEdge == cfaEdge.getEdgeType()) {
      AStatement statement = ((AStatementEdge) cfaEdge).getStatement();
      if (statement instanceof AFunctionCall) {
        AExpression functionNameExp =
            ((AFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression) {
          return THREAD_EXIT.equals(((AIdExpression) functionNameExp).getName());
        }
      }
    }
    return false;
  }

  /** the whole program will terminate after this edge */
  private static boolean isTerminatingEdge(CFAEdge edge) {
    return edge.getSuccessor() instanceof CFATerminationNode;
  }

  /** the whole program will terminate after this edge */
  private boolean isEndOfMainFunction(CFAEdge edge) {
    return Objects.equals(cfa.getMainFunction().getExitNode(), edge.getSuccessor());
  }

  private ThreadingState exitThreads(ThreadingState tmp) {
    // clean up exited threads.
    // this is done before applying any other step.
    for (String id : tmp.getThreadIds()) {
      if (isLastNodeOfThread(tmp.getThreadLocation(id).getLocationNode())) {
        tmp = removeThreadId(tmp, id);
      }
    }
    return tmp;
  }

  /** remove the thread-id from the state, and cleanup remaining locks of this thread. */
  private ThreadingState removeThreadId(ThreadingState ts, final String id) {
    if (useLocalAccessLocks) {
      ts = ts.removeLockAndCopy(id, LOCAL_ACCESS_LOCK);
    }
    if (ts.hasLockForThread(id)) {
      logger.log(Level.WARNING, "dying thread", id, "has remaining locks in state", ts);
    }
    return ts.removeThreadAndCopy(id);
  }

  private Collection<ThreadingState> startNewThread(
      final ThreadingState threadingState,
      final AStatement statement,
      final Collection<ThreadingState> results,
      final CFAEdge cfaEdge)
      throws UnrecognizedCodeException, InterruptedException {

    // first check for some possible errors and unsupported parts
    List<? extends AExpression> params =
        ((AFunctionCall) statement).getFunctionCallExpression().getParameterExpressions();
    if (!(params.get(0) instanceof CUnaryExpression)) {
      throw new UnrecognizedCodeException("unsupported thread assignment", params.get(0));
    }
    if (!(params.get(2) instanceof CUnaryExpression)) {
      throw new UnrecognizedCodeException("unsupported thread function call", params.get(2));
    }
    CExpression expr0 = ((CUnaryExpression) params.get(0)).getOperand();
    CExpression expr2 = ((CUnaryExpression) params.get(2)).getOperand();
    if (!(expr0 instanceof CIdExpression)) {
      throw new UnrecognizedCodeException("unsupported thread assignment", expr0);
    }
    if (!(expr2 instanceof CIdExpression)) {
      throw new UnrecognizedCodeException("unsupported thread function call", expr2);
    }
    if (!(params.get(3) instanceof CExpression)) {
      throw new UnrecognizedCodeException("unsupported thread function argument", params.get(3));
    }

    // now create the thread
    CIdExpression id = (CIdExpression) expr0;
    CIdExpression function = (CIdExpression) expr2;
    CExpression threadArg = (CExpression) params.get(3);

    if (useAllPossibleClones) {
      // for witness validation we need to produce all possible successors,
      // the witness automaton should then limit their number by checking function-entries..
      final Collection<ThreadingState> newResults = new ArrayList<>();
      Set<Integer> usedNumbers = threadingState.getThreadNums();
      for (int i = ThreadingState.MIN_THREAD_NUM; i < maxNumberOfThreads; i++) {
        if (!usedNumbers.contains(i)) {
          newResults.addAll(
              createThreadWithNumber(threadingState, id, cfaEdge, function, threadArg, i, results));
        }
      }
      return newResults;

    } else {
      // a default reachability analysis can determine the thread-number on its own.
      int newThreadNum = threadingState.getSmallestMissingThreadNum();
      return createThreadWithNumber(
          threadingState, id, cfaEdge, function, threadArg, newThreadNum, results);
    }
  }

  private Collection<ThreadingState> createThreadWithNumber(
      final ThreadingState threadingState,
      final CIdExpression id,
      final CFAEdge cfaEdge,
      final CIdExpression function,
      final CExpression threadArg,
      final int newThreadNum,
      final Collection<ThreadingState> results)
      throws UnrecognizedCodeException, InterruptedException {

    String functionName = function.getName();
    if (useClonedFunctions) {
      functionName = CFACloner.getFunctionName(functionName, newThreadNum);
    }

    String threadId = getNewThreadId(threadingState, id.getName());

    // update all successors with a new started thread
    final Collection<ThreadingState> newResults = new ArrayList<>();
    for (ThreadingState ts : results) {
      ThreadingState newThreadingState = addNewThread(ts, threadId, newThreadNum, functionName);
      if (null != newThreadingState) {
        // create a function call for the thread creation
        CFunctionCallEdge functionCall =
            createThreadEntryFunctionCall(
                cfaEdge, function.getExpressionType(), functionName, threadArg);
        newResults.add(newThreadingState.withEntryFunction(functionCall));
      }
    }
    return newResults;
  }

  /**
   * Create a functioncall expression for the thread creation.
   *
   * <p>For `pthread_create(t, ?, foo, arg)` with we return `foo(arg)`.
   *
   * @param cfaEdge where pthread_create was called
   * @param type return-type of the called function
   * @param functionName the (maybe indexed) name of the called function
   * @param arg the argument given to the called function
   */
  private CFunctionCallEdge createThreadEntryFunctionCall(
      final CFAEdge cfaEdge, final CType type, final String functionName, final CExpression arg) {
    CFunctionEntryNode functioncallNode =
        (CFunctionEntryNode)
            Preconditions.checkNotNull(
                cfa.getFunctionHead(functionName),
                "Function '"
                    + functionName
                    + "' was not found. Please enable cloning for the CFA!");
    CFunctionDeclaration functionDeclaration =
        (CFunctionDeclaration) functioncallNode.getFunction();
    CIdExpression functionId =
        new CIdExpression(cfaEdge.getFileLocation(), type, functionName, functionDeclaration);
    CFunctionCallExpression functionCallExpr =
        new CFunctionCallExpression(
            cfaEdge.getFileLocation(),
            type,
            functionId,
            ImmutableList.of(arg),
            functionDeclaration);
    CFunctionCall functionCall =
        new CFunctionCallStatement(cfaEdge.getFileLocation(), functionCallExpr);
    CFunctionCallEdge edge =
        new CFunctionCallEdge(
            functionCallExpr.toASTString(),
            cfaEdge.getFileLocation(),
            cfaEdge.getSuccessor(),
            functioncallNode,
            functionCall,
            null);
    return edge;
  }

  /**
   * returns a new state with a new thread added to the given state.
   *
   * @param threadingState the previous state where to add the new thread
   * @param threadId a unique identifier for the new thread
   * @param newThreadNum a unique number for the new thread
   * @param functionName the main-function of the new thread
   * @return a threadingState with the new thread, or {@code null} if the new thread cannot be
   *     created.
   */
  @Nullable ThreadingState addNewThread(
      ThreadingState threadingState, String threadId, int newThreadNum, String functionName)
      throws InterruptedException {
    CFANode functioncallNode =
        Preconditions.checkNotNull(
            cfa.getFunctionHead(functionName),
            "Function '" + functionName + "' was not found. Please enable cloning for the CFA!");
    AbstractState initialStack =
        callstackCPA.getInitialState(functioncallNode, StateSpacePartition.getDefaultPartition());
    AbstractState initialLoc =
        locationCPA.getInitialState(functioncallNode, StateSpacePartition.getDefaultPartition());

    if (maxNumberOfThreads == -1 || threadingState.getThreadIds().size() < maxNumberOfThreads) {
      threadingState =
          threadingState.addThreadAndCopy(threadId, newThreadNum, initialStack, initialLoc);
      return threadingState;
    } else {
      logger.logfOnce(
          Level.WARNING, "number of threads is limited, cannot create thread %s", threadId);
      return null;
    }
  }

  /** returns the threadId if possible, else the next indexed threadId. */
  private String getNewThreadId(final ThreadingState threadingState, final String threadId)
      throws UnrecognizedCodeException {
    if (!allowMultipleLHS && threadingState.getThreadIds().contains(threadId)) {
      throw new UnrecognizedCodeException(
          "multiple thread assignments to same LHS not supported: " + threadId, null, null);
    }
    String newThreadId = threadId;
    int index = 0;
    while (threadingState.getThreadIds().contains(newThreadId)
        && (maxNumberOfThreads == -1 || index < maxNumberOfThreads)) {
      index++;
      newThreadId = threadId + THREAD_ID_SEPARATOR + index;
      logger.logfOnce(
          Level.WARNING,
          "multiple thread assignments to same LHS, " + "using identifier %s instead of %s",
          newThreadId,
          threadId);
    }
    return newThreadId;
  }

  private Collection<ThreadingState> addLock(
      final ThreadingState threadingState,
      final String activeThread,
      String lockId,
      final Collection<ThreadingState> results) {
    if (threadingState.hasLock(lockId)) {
      // some thread (including activeThread) has the lock, using it twice is not possible
      return ImmutableSet.of();
    }

    return transform(results, ts -> ts.addLockAndCopy(activeThread, lockId));
  }

  /** get the name (lockId) of the new lock at the given edge, or NULL if no lock is required. */
  static @Nullable String getLockId(final CFAEdge cfaEdge) throws UnrecognizedCodeException {
    if (cfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      final AStatement statement = ((AStatementEdge) cfaEdge).getStatement();
      if (statement instanceof AFunctionCall) {
        final AExpression functionNameExp =
            ((AFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression) {
          final String functionName = ((AIdExpression) functionNameExp).getName();
          if (THREAD_MUTEX_LOCK.equals(functionName)) {
            return extractLockId(statement);
          }
        }
      }
    }
    // otherwise no lock is required
    return null;
  }

  private static String extractLockId(final AStatement statement) throws UnrecognizedCodeException {
    // first check for some possible errors and unsupported parts
    List<? extends AExpression> params =
        ((AFunctionCall) statement).getFunctionCallExpression().getParameterExpressions();
    if (!(params.get(0) instanceof CUnaryExpression)) {
      throw new UnrecognizedCodeException("unsupported thread locking", params.get(0));
    }
    //  CExpression expr0 = ((CUnaryExpression)params.get(0)).getOperand();
    //  if (!(expr0 instanceof CIdExpression)) {
    //    throw new UnrecognizedCodeException("unsupported thread lock assignment", expr0);
    //  }
    //  String lockId = ((CIdExpression) expr0).getName();

    String lockId = ((CUnaryExpression) params.get(0)).getOperand().toString();
    return lockId;
  }

  private Collection<ThreadingState> removeLock(
      final String activeThread, final String lockId, final Collection<ThreadingState> results) {
    return transform(results, ts -> ts.removeLockAndCopy(activeThread, lockId));
  }

  private Collection<ThreadingState> joinThread(
      ThreadingState threadingState, AStatement statement, Collection<ThreadingState> results)
      throws UnrecognizedCodeException {

    if (threadingState.getThreadIds().contains(extractParamName(statement, 0))) {
      // we wait for an active thread -> nothing to do
      return ImmutableSet.of();
    }

    return results;
  }

  /** extract the name of the n-th parameter from a function call. */
  static String extractParamName(AStatement statement, int n) throws UnrecognizedCodeException {
    // first check for some possible errors and unsupported parts
    List<? extends AExpression> params =
        ((AFunctionCall) statement).getFunctionCallExpression().getParameterExpressions();
    AExpression expr = params.get(n);
    if (!(expr instanceof CIdExpression)) {
      throw new UnrecognizedCodeException("unsupported thread join access", expr);
    }

    return ((CIdExpression) expr).getName();
  }

  /**
   * optimization for interleaved threads. When a thread only accesses local variables, we ignore
   * other threads and add an internal 'atomic' lock.
   *
   * @return updated state if possible, else NULL.
   */
  private @Nullable ThreadingState handleLocalAccessLock(
      CFAEdge cfaEdge, final ThreadingState threadingState, String activeThread) {

    // check if local access lock exists and is set for current thread
    if (threadingState.hasLock(LOCAL_ACCESS_LOCK)
        && !threadingState.hasLock(activeThread, LOCAL_ACCESS_LOCK)) {
      return null;
    }

    // add local access lock, if necessary and possible
    final boolean isImporantForThreading =
        globalAccessChecker.hasGlobalAccess(cfaEdge) || isImporantForThreading(cfaEdge);
    if (isImporantForThreading) {
      return threadingState.removeLockAndCopy(activeThread, LOCAL_ACCESS_LOCK);
    } else {
      return threadingState.addLockAndCopy(activeThread, LOCAL_ACCESS_LOCK);
    }
  }

  private static boolean isImporantForThreading(CFAEdge cfaEdge) {
    switch (cfaEdge.getEdgeType()) {
      case StatementEdge:
        {
          AStatement statement = ((AStatementEdge) cfaEdge).getStatement();
          if (statement instanceof AFunctionCall) {
            AExpression functionNameExp =
                ((AFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
            if (functionNameExp instanceof AIdExpression) {
              return THREAD_FUNCTIONS.contains(((AIdExpression) functionNameExp).getName());
            }
          }
          return false;
        }
      case FunctionCallEdge:
        // @Deprecated, for old benchmark tasks
        return cfaEdge.getSuccessor().getFunctionName().startsWith(VERIFIER_ATOMIC_BEGIN);
      case FunctionReturnEdge:
        // @Deprecated, for old benchmark tasks
        return cfaEdge.getPredecessor().getFunctionName().startsWith(VERIFIER_ATOMIC_END);
      default:
        return false;
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    Optional<ThreadingState> results = Optional.of((ThreadingState) state);

    for (AutomatonState automatonState :
        AbstractStates.projectToType(otherStates, AutomatonState.class)) {
      if ("WitnessAutomaton".equals(automatonState.getOwningAutomatonName())) {
        results = results.map(ts -> handleWitnessAutomaton(ts, automatonState));
      }
    }

    // delete temporary information from the state, cf. JavaDoc of the called methods
    return Optionals.asSet(results.map(ts -> ts.withActiveThread(null).withEntryFunction(null)));
  }

  private @Nullable ThreadingState handleWitnessAutomaton(
      ThreadingState ts, AutomatonState automatonState) {
    Map<String, AutomatonVariable> vars = automatonState.getVars();
    AutomatonVariable witnessThreadId = vars.get(KeyDef.THREADID.toString().toUpperCase());
    String threadId = ts.getActiveThread();
    if (witnessThreadId == null || threadId == null || witnessThreadId.getValue() == 0) {
      // values not available or default value zero -> ignore and return state unchanged
      return ts;
    }

    Integer witnessId = ts.getThreadIdForWitness(threadId);
    if (witnessId == null) {
      if (ts.hasWitnessIdForThread(witnessThreadId.getValue())) {
        // state contains a mapping, but not for current thread -> wrong branch?
        // TODO returning NULL here would be nice, but leads to unaccepted witnesses :-(
        return ts;
      } else {
        // we know nothing, but can store the new mapping in the state
        return ts.setThreadIdForWitness(threadId, witnessThreadId.getValue());
      }
    }
    if (witnessId.equals(witnessThreadId.getValue())) {
      // corrent branch
      return ts;
    } else {
      // threadId does not match -> no successor
      return ts;
    }
  }

  /** if the current edge creates a new function, return its name, else nothing. */
  public static Optional<String> getCreatedThreadFunction(final CFAEdge edge)
      throws UnrecognizedCodeException {
    if (edge instanceof AStatementEdge) {
      AStatement statement = ((AStatementEdge) edge).getStatement();
      if (statement instanceof AFunctionCall) {
        AExpression functionNameExp =
            ((AFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression) {
          final String functionName = ((AIdExpression) functionNameExp).getName();
          if (ThreadingTransferRelation.THREAD_START.equals(functionName)) {
            List<? extends AExpression> params =
                ((AFunctionCall) statement).getFunctionCallExpression().getParameterExpressions();
            if (!(params.get(2) instanceof CUnaryExpression)) {
              throw new UnrecognizedCodeException(
                  "unsupported thread function call", params.get(2));
            }
            CExpression expr2 = ((CUnaryExpression) params.get(2)).getOperand();
            if (!(expr2 instanceof CIdExpression)) {
              throw new UnrecognizedCodeException("unsupported thread function call", expr2);
            }
            String newThreadFunctionName = ((CIdExpression) expr2).getName();
            return Optional.of(newThreadFunctionName);
          }
        }
      }
    }
    return Optional.empty();
  }
}
