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
package org.sosy_lab.cpachecker.cpa.simplethread;

import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationTM;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.simplethread.SimpleThreadState.SimpleThreadStateBuilder;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "cpa.thread")
public class SimpleThreadTransferRelation extends SingleEdgeTransferRelation implements TransferRelationTM {
  private final TransferRelation locationTransfer;
  private final CallstackTransferRelation callstackTransfer;
  private final SimpleThreadCPAStatistics threadStatistics;

  @Option(secure = true, description = "The case when the same thread is created several times we do not support." +
      "We may skip or fail in this case.")
  private boolean skipTheSameThread = false;

  public SimpleThreadTransferRelation(TransferRelation l,
      TransferRelation c, Configuration pConfiguration) throws InvalidConfigurationException {
    pConfiguration.inject(this);
    locationTransfer = l;
    callstackTransfer = (CallstackTransferRelation) c;
    threadStatistics = new SimpleThreadCPAStatistics();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(AbstractState pState,
      Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {

    threadStatistics.transfer.start();
    SimpleThreadState tState = (SimpleThreadState)pState;
    LocationState oldLocationState = tState.getLocationState();
    CallstackState oldCallstackState = tState.getCallstackState();

    boolean resetCallstacksFlag = false;

    SimpleThreadStateBuilder builder = tState.getBuilder();
    try {
      threadStatistics.tSetTimer.start();
      try {
        if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          if (!handleFunctionCall((CFunctionCallEdge)pCfaEdge, builder)) {
            //Try to join non-created thread
            return Collections.emptySet();
          }
        } else if (pCfaEdge instanceof CFunctionSummaryStatementEdge) {
          CFunctionCall functionCall = ((CFunctionSummaryStatementEdge)pCfaEdge).getFunctionCall();
          if (isThreadCreateFunction(functionCall)) {
            resetCallstacksFlag = true;
            callstackTransfer.enableRecursiveContext();
          }
        } else if (pCfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
          CStatement stmnt = ((CStatementEdge) pCfaEdge).getStatement();
          if (stmnt instanceof CThreadJoinStatement) {
            threadStatistics.threadJoins.inc();
            if (!builder.joinThread((CThreadJoinStatement) stmnt)) {
              return Collections.emptySet();
            }
          }
        } else if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          CFunctionCall functionCall = ((CFunctionReturnEdge)pCfaEdge).getSummaryEdge().getExpression();
          if (isThreadCreateFunction(functionCall)) {
            return Collections.emptySet();
          }
        }
      } catch (CPATransferException e) {
        if (skipTheSameThread) {
          return Collections.emptySet();
        } else {
          throw e;
        }
      } finally {
        threadStatistics.tSetTimer.stop();
      }

      threadStatistics.internalCPAtimer.start();
      threadStatistics.internalLocationTimer.start();
      Collection<? extends AbstractState> newLocationStates = locationTransfer.getAbstractSuccessorsForEdge(oldLocationState,
          SingletonPrecision.getInstance(), pCfaEdge);
      threadStatistics.internalLocationTimer.stop();
      threadStatistics.internalCallstackTimer.start();
      Collection<? extends AbstractState> newCallstackStates = callstackTransfer.getAbstractSuccessorsForEdge(oldCallstackState,
          SingletonPrecision.getInstance(), pCfaEdge);
      threadStatistics.internalCallstackTimer.stop();
      threadStatistics.internalCPAtimer.stop();


      Set<SimpleThreadState> resultStates = new HashSet<>();
      for (AbstractState lState : newLocationStates) {
        for (AbstractState cState : newCallstackStates) {
          resultStates.add(builder.build((LocationState)lState, (CallstackState)cState));
        }
      }
      if (resetCallstacksFlag) {
        callstackTransfer.disableRecursiveContext();
      }
      return resultStates;
    } finally {
      threadStatistics.transfer.stop();
    }
  }

  private boolean handleFunctionCall(CFunctionCallEdge pCfaEdge,
      SimpleThreadStateBuilder builder) throws CPATransferException {

    boolean success = true;
    CFunctionCall fCall = pCfaEdge.getSummaryEdge().getExpression();
    if (isThreadCreateFunction(fCall)) {
      threadStatistics.threadCreates.inc();
      threadStatistics.createdThreads.add(pCfaEdge.getSuccessor().getFunctionName());
      builder.createThread((CThreadCreateStatement) fCall);
      //Just to statistics
      threadStatistics.maxNumberOfThreads.setNextValue(builder.getThreadSize());
    } else if (isThreadJoinFunction(fCall)) {
      threadStatistics.threadJoins.inc();
      success = builder.joinThread((CThreadJoinStatement)fCall);
    }
    return success;
  }

  private boolean isThreadCreateFunction(CFunctionCall statement) {
    return (statement instanceof CThreadCreateStatement);
  }

  private boolean isThreadJoinFunction(CFunctionCall statement) {
    return (statement instanceof CThreadJoinStatement);
  }

  public Statistics getStatistics() {
    return threadStatistics;
  }

  @Override
  public Collection<Pair<AbstractState, InferenceObject>> getAbstractSuccessors(AbstractState pState, InferenceObject pInferenceObject, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (pInferenceObject == TauInferenceObject.getInstance()) {
      return from(getAbstractSuccessors(pState, pPrecision))
          .transform(s -> Pair.of((AbstractState) s, (InferenceObject) SimpleThreadInferenceObject.create((SimpleThreadState) s)))
          .toSet();
    } else {
      return Collections.singleton(Pair.of(pState, EmptyInferenceObject.getInstance()));
    }
  }

  @Override
  public Collection<Pair<AbstractState, InferenceObject>> getAbstractSuccessorForEdge(AbstractState pState, InferenceObject pInferenceObject, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    if (pInferenceObject == TauInferenceObject.getInstance()) {
      return from(getAbstractSuccessorsForEdge(pState, pPrecision, pCfaEdge))
          .transform(s -> Pair.of((AbstractState) s, (InferenceObject) SimpleThreadInferenceObject.create((SimpleThreadState) s)))
          .toSet();
    } else {
      return Collections.singleton(Pair.of(pState, EmptyInferenceObject.getInstance()));
    }
  }
}
