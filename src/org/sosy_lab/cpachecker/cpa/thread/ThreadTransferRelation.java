// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadJoinStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.thread.ThreadAbstractEdge.ThreadAction;
import org.sosy_lab.cpachecker.cpa.thread.ThreadState.ThreadStatus;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "cpa.thread")
public class ThreadTransferRelation implements TransferRelation {
  @Option(
    secure = true,
    description = "The case when the same thread is created several times we do not support."
        + "We may skip or fail in this case.")
  private boolean skipTheSameThread = false;

  @Option(
    secure = true,
    description = "The case when the same thread is created several times we do not support."
        + "We may try to support it with self-parallelizm.")
  private boolean supportSelfCreation = false;

  private final ThreadCPAStatistics threadStatistics;

  public ThreadTransferRelation(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    threadStatistics = new ThreadCPAStatistics();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(AbstractState pState,
      Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {

    threadStatistics.transfer.start();
    ThreadState tState = (ThreadState)pState;
    ThreadState newState = tState;

    try {
      if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        newState = handleFunctionCall(tState, (CFunctionCallEdge) pCfaEdge);
      } else if (pCfaEdge instanceof CFunctionSummaryStatementEdge) {
        CFunctionCall functionCall = ((CFunctionSummaryStatementEdge) pCfaEdge).getFunctionCall();
        if (isThreadCreateFunction(functionCall)) {
          newState = handleParentThread(tState, (CThreadCreateStatement) functionCall);
        }
      } else if (pCfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
        CStatement stmnt = ((CStatementEdge) pCfaEdge).getStatement();
        if (stmnt instanceof CThreadJoinStatement) {
          threadStatistics.threadJoins.inc();
          newState = joinThread(tState, (CThreadJoinStatement) stmnt);
        }
      } else if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        CFunctionCall functionCall =
            ((CFunctionReturnEdge) pCfaEdge).getSummaryEdge().getExpression();
        if (isThreadCreateFunction(functionCall)) {
          newState = null;
        }
      }
      if (newState != null) {
        return Collections.singleton(newState);
      } else {
        return ImmutableSet.of();
      }
    } catch (CPATransferException e) {
      if (skipTheSameThread) {
        return ImmutableSet.of();
      } else {
        throw e;
      }
    } finally {
      threadStatistics.transfer.stop();
    }
  }

  private ThreadState handleFunctionCall(
      ThreadState state,
      CFunctionCallEdge pCfaEdge)
      throws CPATransferException {

    ThreadState newState = state;
    CFunctionCall fCall = pCfaEdge.getSummaryEdge().getExpression();
    if (isThreadCreateFunction(fCall)) {
      newState = handleChildThread(state, (CThreadCreateStatement) fCall);
      if (threadStatistics.createdThreads.add(pCfaEdge.getSuccessor().getFunctionName())) {
        threadStatistics.threadCreates.inc();
        // Just to statistics
        threadStatistics.maxNumberOfThreads.setNextValue(state.getThreadSize());
      }
    } else if (isThreadJoinFunction(fCall)) {
      threadStatistics.threadJoins.inc();
      newState = joinThread(state, (CThreadJoinStatement) fCall);
    }
    return newState;
  }

  private ThreadState handleParentThread(ThreadState state, CThreadCreateStatement tCall)
      throws CPATransferException {
    return createThread(state, tCall, ThreadStatus.PARENT_THREAD);
  }

  private ThreadState handleChildThread(ThreadState state, CThreadCreateStatement tCall)
      throws CPATransferException {
    return createThread(
        state,
        tCall,
        tCall.isSelfParallel() ? ThreadStatus.SELF_PARALLEL_THREAD : ThreadStatus.CREATED_THREAD);
  }

  private ThreadState
      createThread(ThreadState state, CThreadCreateStatement tCall, ThreadStatus pParentThread)
          throws CPATransferException {
    final String pVarName = tCall.getVariableName();
    // Just to info
    final String pFunctionName =
        tCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();

    Map<String, ThreadStatus> tSet = state.getThreadSet();
    ThreadStatus status = pParentThread;
    if (tSet.containsKey(pVarName)) {
      if (supportSelfCreation) {
        status = ThreadStatus.SELF_PARALLEL_THREAD;

      } else {
        throw new CPATransferException(
            "Can not create thread " + pFunctionName + ", it was already created");
      }
    }

    if (!tSet.isEmpty()) {
      if (tSet.get(state.getCurrentThread()) == ThreadStatus.SELF_PARALLEL_THREAD) {
        // Can add only the same status
        status = ThreadStatus.SELF_PARALLEL_THREAD;
      }
    }
    Map<String, ThreadStatus> newSet = new TreeMap<>(tSet);
    newSet.put(pVarName, status);
    String current;
    if (pParentThread == ThreadStatus.PARENT_THREAD) {
      current = state.getCurrentThread();
    } else {
      current = pVarName;
    }
    return state.copyWith(current, newSet);
  }

  public ThreadState joinThread(ThreadState state, CThreadJoinStatement jCall) {
    // If we found several labels for different functions
    // it means, that there are several thread created for one thread variable.
    // Not a good situation, but it is not forbidden, so join the last assigned thread
    Map<String, ThreadStatus> tSet = state.getThreadSet();

    String var = jCall.getVariableName();
    if (tSet.containsKey(var) && tSet.get(var) != ThreadStatus.CREATED_THREAD) {
      Map<String, ThreadStatus> newSet = new TreeMap<>(tSet);
      newSet.remove(var);
      return state.copyWith(state.getCurrentThread(), newSet);
    }
    return state;
  }

  public static boolean isThreadCreateFunction(CFunctionCall statement) {
    return (statement instanceof CThreadCreateStatement);
  }

  private boolean isThreadJoinFunction(CFunctionCall statement) {
    return (statement instanceof CThreadJoinStatement);
  }

  public Statistics getStatistics() {
    return threadStatistics;
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessors(AbstractState pState, Precision pPrecision)
          throws CPATransferException, InterruptedException {

    ThreadTMStateWithEdge stateWithEdge = (ThreadTMStateWithEdge) pState;
    ThreadAbstractEdge edge = stateWithEdge.getAbstractEdge();
    Map<String, ThreadStatus> tSet = stateWithEdge.getThreadSet();

    if (edge != null) {
      ThreadAction action = edge.getAction().getFirst();
      String threadName = edge.getAction().getSecond();

      // TMP implementation
      ThreadStatus status = ThreadStatus.CREATED_THREAD;
      Map<String, ThreadStatus> newSet = new TreeMap<>(tSet);
      if (action == ThreadAction.CREATE) {
        if (tSet.containsKey(threadName)) {
          // Means, we have already apply the create thread
          return Collections
              .singleton(stateWithEdge.copyWith(stateWithEdge.getCurrentThread(), tSet));
        }
        newSet.put(threadName, status);
      } else if (action == ThreadAction.JOIN) {
        if (stateWithEdge.getCurrentThread().equals(threadName)) {
          // Means someone wants to join current thread. Stops the branch, as the case is
          // impossible
          return ImmutableList.of();
        }
        newSet.remove(threadName);
      } else {
        throw new UnsupportedOperationException("Unsupported action " + action);
      }
      // Save the current, do not recompute it!
      return Collections
          .singleton(stateWithEdge.copyWith(stateWithEdge.getCurrentThread(), newSet));
    }

    // To reset the edge
    return Collections.singleton(stateWithEdge.copyWith(stateWithEdge.getCurrentThread(), tSet));
  }
}
