// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.thread;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.cpa.thread.ThreadState.ThreadStatus;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "cpa.thread")
public class ThreadTransferRelation extends SingleEdgeTransferRelation {
  @Option(
      secure = true,
      description =
          "The case when the same thread is created several times we do not support."
              + "We may skip or fail in this case.")
  private boolean skipTheSameThread = false;

  @Option(
      secure = true,
      description =
          "The case when the same thread is created several times we do not support."
              + "We may try to support it with self-parallelizm.")
  private boolean supportSelfCreation = false;

  @Option(secure = true, description = "Simple thread analysis from theory paper")
  private boolean simpleMode = false;

  private final ThreadCPAStatistics threadStatistics;

  public ThreadTransferRelation(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    threadStatistics = new ThreadCPAStatistics();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    threadStatistics.transfer.start();
    ThreadState tState = (ThreadState) pState;
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

  private ThreadState handleFunctionCall(ThreadState state, CFunctionCallEdge pCfaEdge)
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

  private ThreadState createThread(
      ThreadState state, CThreadCreateStatement tCall, ThreadStatus pParentThread)
      throws CPATransferException {
    final String pVarName = tCall.getVariableName();
    // Just to info
    final String pFunctionName =
        tCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();

    Map<String, ThreadStatus> tSet = state.getThreadSet();
    List<ThreadLabel> order = state.getOrder();
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
      ThreadLabel last = order.get(order.size() - 1);
      if (tSet.get(last.getVarName()) == ThreadStatus.SELF_PARALLEL_THREAD) {
        // Can add only the same status
        status = ThreadStatus.SELF_PARALLEL_THREAD;
      }
    }
    ThreadLabel label = new ThreadLabel(pFunctionName, pVarName);
    Map<String, ThreadStatus> newSet;
    List<ThreadLabel> newOrder;
    if (simpleMode) {
      // Store only current creation
      newSet = new TreeMap<>();
      newOrder = new ArrayList<>();
    } else {
      newSet = new TreeMap<>(tSet);
      newOrder = new ArrayList<>(order);
    }
    newSet.put(pVarName, status);
    newOrder.add(label);
    return new ThreadState(newSet, state.getRemovedSet(), newOrder);
  }

  public ThreadState joinThread(ThreadState state, CThreadJoinStatement jCall) {
    // If we found several labels for different functions
    // it means, that there are several thread created for one thread variable.
    // Not a good situation, but it is not forbidden, so join the last assigned thread
    List<ThreadLabel> order = state.getOrder();
    Map<String, ThreadStatus> tSet = state.getThreadSet();

    Optional<ThreadLabel> result =
        Lists.reverse(order).stream()
            .filter(l -> l.getVarName().equals(jCall.getVariableName()))
            .findFirst();
    // Do not self-join
    if (result.isPresent()) {
      ThreadLabel toRemove = result.orElseThrow();
      String var = toRemove.getVarName();
      if (tSet.containsKey(var) && tSet.get(var) != ThreadStatus.CREATED_THREAD) {
        Map<String, ThreadStatus> newSet = new TreeMap<>(tSet);
        List<ThreadLabel> newOrder = new ArrayList<>(order);
        newSet.remove(var);
        newOrder.remove(toRemove);
        return new ThreadState(newSet, state.getRemovedSet(), newOrder);
      }
    }
    return state;
  }

  private boolean isThreadCreateFunction(CFunctionCall statement) {
    return statement instanceof CThreadCreateStatement;
  }

  private boolean isThreadJoinFunction(CFunctionCall statement) {
    return statement instanceof CThreadJoinStatement;
  }

  public Statistics getStatistics() {
    return threadStatistics;
  }
}
