/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.thread;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.thread.ThreadLabel.LabelStatus;
import org.sosy_lab.cpachecker.cpa.thread.ThreadState.ThreadStateBuilder;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;


public class ThreadTransferRelation extends SingleEdgeTransferRelation {
  private final TransferRelation locationTransfer;
  private final TransferRelation callstackTransfer;
  private final ThreadCPAStatistics threadStatistics;

  private static String JOIN = "ldv_thread_join";
  private static String JOIN_SELF_PARALLEL = "ldv_thread_join_N";

  private boolean resetCallstacksFlag;

  public ThreadTransferRelation(TransferRelation l,
      TransferRelation c, Configuration pConfiguration) {
    locationTransfer = l;
    callstackTransfer = c;
    threadStatistics = new ThreadCPAStatistics();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(AbstractState pState,
      Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {

    threadStatistics.transfer.start();
    ThreadState tState = (ThreadState)pState;
    LocationState oldLocationState = tState.getLocationState();
    CallstackState oldCallstackState = tState.getCallstackState();

    ThreadStateBuilder builder = tState.getBuilder();
    try {
      if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          if (!handleFunctionCall((CFunctionCallEdge)pCfaEdge, builder)) {
            //Try to join non-created thread
            threadStatistics.transfer.stop();
            return Collections.emptySet();
          }
      } else if (pCfaEdge instanceof CFunctionSummaryStatementEdge) {
        CFunctionCall functionCall = ((CFunctionSummaryStatementEdge)pCfaEdge).getFunctionCall();
        if (isThreadCreateFunction(functionCall)) {
          String functionName = ((CFunctionSummaryStatementEdge)pCfaEdge).getFunctionName();
          builder.addToThreadSet(new ThreadLabel(functionName, LabelStatus.PARENT_THREAD));
          resetCallstacksFlag = true;
          ((CallstackTransferRelation)callstackTransfer).enableRecursiveContext();
        }
      } else if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        CFunctionCall functionCall = ((CFunctionReturnEdge)pCfaEdge).getSummaryEdge().getExpression();
        if (isThreadCreateFunction(functionCall)) {
          threadStatistics.transfer.stop();
          return Collections.emptySet();
        }
      }
    } catch (HandleCodeException e) {
      //throw new CPATransferException(e.getMessage());
      threadStatistics.transfer.stop();
      return Collections.emptySet();
    }

    Collection<? extends AbstractState> newLocationStates = locationTransfer.getAbstractSuccessorsForEdge(oldLocationState,
        SingletonPrecision.getInstance(), pCfaEdge);
    Collection<? extends AbstractState> newCallstackStates = callstackTransfer.getAbstractSuccessorsForEdge(oldCallstackState,
        SingletonPrecision.getInstance(), pCfaEdge);


    Set<ThreadState> resultStates = new HashSet<>();
    for (AbstractState lState : newLocationStates) {
      for (AbstractState cState : newCallstackStates) {
        builder.setWrappedStates((LocationState)lState, (CallstackState)cState);
        resultStates.add(builder.build());
      }
    }
    if (resetCallstacksFlag) {
      ((CallstackTransferRelation)callstackTransfer).disableRecursiveContext();
      resetCallstacksFlag = false;
    }
    threadStatistics.transfer.stop();
    return resultStates;
  }

  private boolean handleFunctionCall(CFunctionCallEdge pCfaEdge,
      ThreadStateBuilder builder) throws HandleCodeException {
    String functionName = pCfaEdge.getSuccessor().getFunctionName();

    boolean success = true;
    CFunctionCall fCall = pCfaEdge.getSummaryEdge().getExpression();
    if (isThreadCreateFunction(fCall)) {
      threadStatistics.threadCreates.inc();
      LabelStatus status =  ((CThreadCreateStatement)fCall).isSelfParallel() ? LabelStatus.SELF_PARALLEL_THREAD : LabelStatus.CREATED_THREAD;
      builder.addToThreadSet(new ThreadLabel(functionName, status));
      //Just to statistics
      ThreadState tmpState = builder.build();
      threadStatistics.maxNumberOfThreads.setNextValue(tmpState.getThreadSet().size());
    } else if (functionName.equals(JOIN) || functionName.equals(JOIN_SELF_PARALLEL)) {
      threadStatistics.threadJoins.inc();
      List<CExpression> args = pCfaEdge.getArguments();
      functionName = ((CUnaryExpression)args.get(1)).getOperand().toASTString();
      success = builder.removeFromThreadSet(new ThreadLabel(functionName, LabelStatus.PARENT_THREAD));
    }
    return success;
  }

  private boolean isThreadCreateFunction(CFunctionCall statement) {
    return (statement instanceof CThreadCreateStatement);
    //return functionName.equals(CREATE) || functionName.equals(CREATE_SELF_PARALLEL);
  }

  public Statistics getStatistics() {
    return threadStatistics;
  }
}
