/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.threading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.CFACloner;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

@Options(prefix="cpa.threading")
public final class ThreadingTransferRelation extends SingleEdgeTransferRelation {

  @Option(description="do not use the original functions from the CFA, but cloned ones. "
      + "See cfa.postprocessing.CFACloner for detail.")
  private boolean useClonedFunctions = true;

  @Option(description="the maximal number of parallel threads, -1 for infinite. "
      + "When combined with 'useClonedFunctions=true', we need at least N cloned functions. "
      + "The option 'cfa.cfaCloner.numberOfCopies' should be set to N.")
  private int maxNumberOfThreads = 5;

  @Option(description="atomic locks are used to simulate atomic statements, as described in the rules of SV-Comp.")
  private boolean useAtomicLocks = true;

  public static final String THREAD_START = "pthread_create";
  private static final String THREAD_JOIN = "pthread_join";
  private static final String THREAD_EXIT = "pthread_exit";
  private static final String THREAD_MUTEX_LOCK = "pthread_mutex_lock";
  private static final String THREAD_MUTEX_UNLOCK = "pthread_mutex_unlock";

  private static final String THREAD_ATOMIC_BEGIN = "pthread_atomic_begin";
  private static final String THREAD_ATOMIC_END = "pthread_atomic_end";
  private static final String VERIFIER_ATOMIC_BEGIN = "__VERIFIER_atomic_begin";
  private static final String VERIFIER_ATOMIC_END = "__VERIFIER_atomic_end";
  private static final String ATOMIC_LOCK = "__CPAchecker_atomic_lock__";

  private final CFA cfa;
  private final ConfigurableProgramAnalysis callstackCPA;
  private final ConfigurableProgramAnalysis locationCPA;

  public ThreadingTransferRelation(
      Configuration pConfig, ConfigurableProgramAnalysis pCallstackCPA,
      ConfigurableProgramAnalysis pLocationCPA, CFA pCfa)
          throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    callstackCPA = pCallstackCPA;
    locationCPA = pLocationCPA;
  }

  @Override
  public Collection<ThreadingState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
        throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(cfaEdge);
    Preconditions.checkArgument(!(cfaEdge instanceof MultiEdge),
        "MultiEdges cannot be supported by ThreadingCPA");
    final ThreadingState threadingState = (ThreadingState) state;

    // filter out all states, where the edge is not available
    final Set<String> activeThreads = new HashSet<>();
    for (String id : threadingState.getThreadIds()) {
      Pair<AbstractState, AbstractState> threadPos = threadingState.getThreadLocation(id);
      if (Iterables.contains(((LocationState)threadPos.getSecond()).getOutgoingEdges(), cfaEdge)) {
        activeThreads.add(id);
      }
    }

    assert activeThreads.size() == 1 : "multiple thread active: " + activeThreads;
    // then either the same function is ccalled in different threads -> not supported.
    // (or CompositeCPA and ThreadingCPA do not work together)

    String activeThread = Iterables.getOnlyElement(activeThreads);

    // get all possible successors
    Collection<ThreadingState> results = getAbstractSuccessorsForSimpleEdge(activeThread, threadingState, precision, cfaEdge);

    // check if atomic lock exists and is set for current thread
    if (useAtomicLocks && threadingState.hasLock(ATOMIC_LOCK) && !threadingState.hasLock(activeThread, ATOMIC_LOCK)) {
      return Collections.emptySet();
    }

    // TODO we should exit after analyzing the edge, not before.
    if (isEndOfMainFunction(cfaEdge) ||
        isTerminatingEdge(cfaEdge)) {
      // VERIFIER_assume not only terminates the current thread, but the whole program
      return Collections.emptySet();
    }

    if (isLastEdgeOfThread(cfaEdge)) {
      // if thread does not exit with "pthread_exit"
      results = exitThread(threadingState, activeThread, results);
    }

    switch (cfaEdge.getEdgeType()) {
    case StatementEdge: {
      AStatement statement = ((AStatementEdge)cfaEdge).getStatement();
      if (statement instanceof AFunctionCall) {
        AExpression functionNameExp = ((AFunctionCall)statement).getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression) {
          String functionName = ((AIdExpression)functionNameExp).getName();

          switch(functionName) {
          case THREAD_START:
            results = startNewThread(threadingState, statement, results);
            break;
          case THREAD_MUTEX_LOCK:
            results = addLock(threadingState, activeThread, statement, results);
            break;
          case THREAD_MUTEX_UNLOCK:
            results = removeLock(activeThread, statement, results);
            break;
          case THREAD_JOIN:
            results = joinThread(threadingState, activeThread, statement, results);
            break;
          case THREAD_EXIT:
            results = exitThread(threadingState, activeThread, results);
            break;
          default:
          }
        }
      }
      break;
    }
    case FunctionCallEdge: {
      // cloning changes the function-name -> we use 'startsWith'
      if (useAtomicLocks && cfaEdge.getSuccessor().getFunctionName().startsWith(VERIFIER_ATOMIC_BEGIN)) {
        results = addLock(threadingState, activeThread, ATOMIC_LOCK, results);
      }
      break;
    }
    case FunctionReturnEdge: {
      // cloning changes the function-name -> we use 'startsWith'
      if (useAtomicLocks && cfaEdge.getPredecessor().getFunctionName().startsWith(VERIFIER_ATOMIC_END)) {
        results = removeLock(activeThread, ATOMIC_LOCK, results);
      }
      break;
    }
    default:
      // nothing to do
    }
    return results;
  }

  /** compute successors for the current edge.
   * There will be only one successor in most cases, even with N threads,
   * because the edge limits the transitions to only one thread. */
  private Collection<ThreadingState> getAbstractSuccessorsForSimpleEdge(
      String activeThread, ThreadingState threadingState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    final Collection<ThreadingState> results = new HashSet<>();
    Pair<AbstractState, AbstractState> threadPos = threadingState.getThreadLocation(activeThread);

    // compute new locations
    Collection<? extends AbstractState> newLocs = locationCPA.getTransferRelation().
        getAbstractSuccessorsForEdge(threadPos.getSecond(), precision, cfaEdge);

    // compute new stacks
    Collection<? extends AbstractState> newStacks = callstackCPA.getTransferRelation().
        getAbstractSuccessorsForEdge(threadPos.getFirst(), precision, cfaEdge);

    // combine them pairwise, all combinations needed
    for (AbstractState loc : newLocs) {
      for (AbstractState stack : newStacks) {
        results.add(threadingState.updateThreadAndCopy(activeThread, Pair.of(stack, loc)));
      }
    }

    return results;
  }

  /** the current thread will terminate after this edge */
  private boolean isLastEdgeOfThread(CFAEdge edge) {
    return 0 == edge.getSuccessor().getNumLeavingEdges();
  }

  /** the whole program will terminate after this edge */
  private boolean isTerminatingEdge(CFAEdge edge) {
    return edge.getSuccessor() instanceof CFATerminationNode;
  }

  /** the whole program will terminate after this edge */
  private boolean isEndOfMainFunction(CFAEdge edge) {
    return cfa.getMainFunction().getExitNode() == edge.getSuccessor();
  }

  private Collection<ThreadingState> exitThread(
      final ThreadingState threadingState, final String activeThread,
      final Collection<ThreadingState> results) {

    // update all successors
    final Collection<ThreadingState> newResults = new ArrayList<>();
    for (ThreadingState ts : results) {
      ts = ts.removeThreadAndCopy(activeThread);
      if (ts.getThreadIds().isEmpty()) {
        // we have exited all threads, no successor
      } else {
        newResults.add(ts);
      }
    }
    return newResults;
  }

  private Collection<ThreadingState> startNewThread(
      final ThreadingState threadingState, final AStatement statement,
      final Collection<ThreadingState> results) throws UnrecognizedCodeException {

    // first check for some possible errors and unsupported parts
    List<? extends AExpression> params = ((AFunctionCall)statement).getFunctionCallExpression().getParameterExpressions();
    if (!(params.get(0) instanceof CUnaryExpression)) {
      throw new UnrecognizedCodeException("unsupported thread assignment", params.get(0));
    }
    if (!(params.get(2) instanceof CUnaryExpression)) {
      throw new UnrecognizedCodeException("unsupported thread function call", params.get(2));
    }
    CExpression expr0 = ((CUnaryExpression)params.get(0)).getOperand();
    CExpression expr2 = ((CUnaryExpression)params.get(2)).getOperand();
    if (!(expr0 instanceof CIdExpression)) {
      throw new UnrecognizedCodeException("unsupported thread assignment", expr0);
    }
    if (!(expr2 instanceof CIdExpression)) {
      throw new UnrecognizedCodeException("unsupported thread function call", expr2);
    }

    // now create the thread
    CIdExpression id = (CIdExpression) expr0;
    String functionName = ((CIdExpression) expr2).getName();
    int newThreadNum = threadingState.getSmallestMissingThreadNum();

    if (useClonedFunctions) {
      functionName = CFACloner.getFunctionName(functionName, newThreadNum);
    }

    CFANode functioncallNode = Preconditions.checkNotNull(cfa.getFunctionHead(functionName), functionName);
    Pair<AbstractState, AbstractState> p = Pair.of(
        callstackCPA.getInitialState(functioncallNode, StateSpacePartition.getDefaultPartition()),
        locationCPA.getInitialState(functioncallNode, StateSpacePartition.getDefaultPartition()));

    if (threadingState.getThreadIds().contains(id.getName())) {
      throw new UnrecognizedCodeException("multiple thread assignments to same LHS not supported", id);
    }

    // update all successors
    final Collection<ThreadingState> newResults = new ArrayList<>();
    for (ThreadingState ts : results) {
      if (maxNumberOfThreads == -1 || ts.getThreadIds().size() <= maxNumberOfThreads) {
        ts = ts.addThreadAndCopy(id.getName(), newThreadNum, p);
        newResults.add(ts);
      }
    }
    return newResults;
  }

  private Collection<ThreadingState> addLock(
      final ThreadingState threadingState, final String activeThread,
      final AStatement statement, final Collection<ThreadingState> results)
          throws UnrecognizedCodeException {

    // first check for some possible errors and unsupported parts
    List<? extends AExpression> params = ((AFunctionCall)statement).getFunctionCallExpression().getParameterExpressions();
    if (!(params.get(0) instanceof CUnaryExpression)) {
      throw new UnrecognizedCodeException("unsupported thread locking", params.get(0));
    }
    CExpression expr0 = ((CUnaryExpression)params.get(0)).getOperand();
    if (!(expr0 instanceof CIdExpression)) {
      throw new UnrecognizedCodeException("unsupported thread lock assignment", expr0);
    }

    String lockId = ((CIdExpression) expr0).getName();

    return addLock(threadingState, activeThread, lockId, results);
  }

  private Collection<ThreadingState> addLock(final ThreadingState threadingState, final String activeThread,
      String lockId, final Collection<ThreadingState> results) {
    if (threadingState.hasLock(lockId)) {
      // some thread (including activeThread) has the lock, using it twice is not possible
      return Collections.emptySet();
    }

    // update all successors
    final Collection<ThreadingState> newResults = new ArrayList<>();
    for (ThreadingState ts : results) {
      ts = ts.addLockAndCopy(activeThread, lockId);
      newResults.add(ts);
    }
    return newResults;
  }

  private Collection<ThreadingState> removeLock(
      final String activeThread,
      final AStatement statement,
      final Collection<ThreadingState> results)
          throws UnrecognizedCodeException {

    // first check for some possible errors and unsupported parts
    List<? extends AExpression> params = ((AFunctionCall)statement).getFunctionCallExpression().getParameterExpressions();
    if (!(params.get(0) instanceof CUnaryExpression)) {
      throw new UnrecognizedCodeException("unsupported thread locking", params.get(0));
    }
    CExpression expr0 = ((CUnaryExpression)params.get(0)).getOperand();
    if (!(expr0 instanceof CIdExpression)) {
      throw new UnrecognizedCodeException("unsupported thread lock assignment", expr0);
    }

    String lockId = ((CIdExpression) expr0).getName();

    return removeLock(activeThread, lockId, results);
  }

  private Collection<ThreadingState> removeLock(
      final String activeThread,
      final String lockId,
      final Collection<ThreadingState> results) {
    // update all successors
    final Collection<ThreadingState> newResults = new ArrayList<>();
    for (ThreadingState ts : results) {
      ts = ts.removeLockAndCopy(activeThread, lockId);
      newResults.add(ts);
    }
    return newResults;
  }

  private Collection<ThreadingState> joinThread(ThreadingState threadingState, String activeThread,
      AStatement statement, Collection<ThreadingState> results) throws UnrecognizedCodeException {

    // first check for some possible errors and unsupported parts
    List<? extends AExpression> params = ((AFunctionCall)statement).getFunctionCallExpression().getParameterExpressions();
    AExpression expr0 = params.get(0);
    if (!(expr0 instanceof CIdExpression)) {
      throw new UnrecognizedCodeException("unsupported thread join access", expr0);
    }

    String threadId = ((CIdExpression) expr0).getName();

    if (threadingState.getThreadIds().contains(threadId)) {
      // we wait for an active thread -> nothing to do
      return Collections.emptySet();
    }

    return results;
  }



  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    // strengthen should not be used with ThreadingTransfer
    return null;
  }
}
