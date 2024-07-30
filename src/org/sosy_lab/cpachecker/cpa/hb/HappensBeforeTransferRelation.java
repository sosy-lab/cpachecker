// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import static org.sosy_lab.cpachecker.util.CFAUtils.allLeavingEdges;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class HappensBeforeTransferRelation extends SingleEdgeTransferRelation {
  private final LocationCPA locationCPA;
  private final CallstackCPA callstackCPA;
  //  private final LogManager logger;
  //  private final Configuration configuration;
  private final CFA cfa;

  public HappensBeforeTransferRelation(Configuration pConfig, CFA pCfa, LogManager pLogger)
      throws InvalidConfigurationException {
    //    pConfig.inject(this);
    //    configuration = pConfig;
    locationCPA = LocationCPA.create(pCfa, pConfig);
    callstackCPA = new CallstackCPA(pConfig, pLogger);
    //    logger = new LogManagerWithoutDuplicates(pLogger);
    cfa = pCfa;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (state instanceof HappensBeforeState prevState) {
      if (cfaEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)
          && cfaEdge.getSuccessor().equals(cfaEdge.getPredecessor())) {
        // likely our pseudo-edge
        return ImmutableList.of(prevState.clearPending());
      }

      switch (cfaEdge.getEdgeType()) {
        case StatementEdge:
          {
            AStatement statement = ((AStatementEdge) cfaEdge).getStatement();
            if (statement instanceof AFunctionCall pAFunctionCall) {
              AExpression functionNameExp =
                  pAFunctionCall.getFunctionCallExpression().getFunctionNameExpression();
              if (functionNameExp instanceof AIdExpression pFunctionName) {
                final String functionName = pFunctionName.getName();
                switch (functionName) {
                  case "pthread_create":
                    final var params =
                        pAFunctionCall.getFunctionCallExpression().getParameterExpressions();
                    Preconditions.checkState(
                        params.size() == 4,
                        "Malformed pthread_create (not 4 params): " + pAFunctionCall);
                    Preconditions.checkState(
                        params.get(2) instanceof CUnaryExpression
                            && ((CUnaryExpression) params.get(2)).getOperator()
                                == UnaryOperator.AMPER,
                        "Malformed pthread_create (Thread not unary expression with reference): "
                            + params.get(2));
                    Preconditions.checkState(
                        ((CUnaryExpression) params.get(2)).getOperand() instanceof CIdExpression,
                        "Malformed pthread_create (Thread not CIdExpression): "
                            + ((CUnaryExpression) params.get(2)).getOperand());
                    prevState =
                        addNewThread(
                            prevState,
                            ((CIdExpression) ((CUnaryExpression) params.get(2)).getOperand())
                                .getName());
                    break;
                  default:
                    // nothing to do
                    break;
                }
              }
            }
            break;
          }
        default:
          {
          }
      }

      final var old = prevState;

      final var thread = old.threads().get(old.nextActiveThread());

      final var nextLocs =
          locationCPA
              .getTransferRelation()
              .getAbstractSuccessorsForEdge(thread.getFirstNotNull(), precision, cfaEdge);
      final var nextStacks =
          callstackCPA
              .getTransferRelation()
              .getAbstractSuccessorsForEdge(thread.getSecondNotNull(), precision, cfaEdge);

      final var nextStates =
          nextLocs.stream()
              .flatMap(
                  nextLoc ->
                      nextStacks.stream()
                          .flatMap(
                              nextStack -> {
                                var base =
                                    old.updateThread(
                                        old.nextActiveThread(),
                                        (LocationState) nextLoc,
                                        (CallstackState) nextStack,
                                        firstCanExecute(
                                            ImmutableMap
                                                .<Integer, Pair<LocationState, CallstackState>>
                                                    builder()
                                                .putAll(old.threads())
                                                .put(
                                                    old.nextActiveThread(),
                                                    Pair.of(
                                                        (LocationState) nextLoc,
                                                        (CallstackState) nextStack))
                                                .buildKeepingLast()),
                                        HappensBeforeEdgeTools.nextCssaCounters(
                                            cfaEdge, old.nextActiveThread(), old.cssaCounters()));
                                final var accesses =
                                    HappensBeforeEdgeTools.getAccesses(
                                        cfaEdge, old.nextActiveThread(), old.cssaCounters());
                                final var writes = accesses.getFirstNotNull();
                                final var reads = accesses.getSecondNotNull();

                                var ret = ImmutableSet.of(base);
                                for (CVariableDeclaration write : writes) {
                                  var newStates = ImmutableSet.<HappensBeforeState>builder();
                                  for (HappensBeforeState happensBeforeState : ret) {
                                    newStates.addAll(
                                        happensBeforeState.addWrite(old.nextActiveThread(), write));
                                  }
                                  ret = newStates.build();
                                }

                                for (CVariableDeclaration it : reads) {

                                  var newStates = ImmutableSet.<HappensBeforeState>builder();
                                  for (HappensBeforeState happensBeforeState : ret) {
                                    newStates.addAll(
                                        happensBeforeState.addRead(old.nextActiveThread(), it));
                                  }
                                  ret = newStates.build();
                                }
                                return ret.stream();
                              }));

      return nextStates.toList();
    } else {
      return ImmutableList.of();
    }
  }

  private int firstCanExecute(Map<Integer, Pair<LocationState, CallstackState>> pThreads) {
    for (Map.Entry<Integer, Pair<LocationState, CallstackState>> entry : pThreads.entrySet()) {
      if (!allLeavingEdges(entry.getValue().getFirstNotNull().getLocationNode()).isEmpty()) {
        return entry.getKey();
      }
    }
    return -1;
  }

  HappensBeforeState addNewThread(final HappensBeforeState old, final String functionName) {
    final int threadId = old.threads().size();
    //    final CFA clonedCFA = cfaCloner.execute(threadId); // the new threadID is |old threads|

    CFANode functioncallNode =
        Preconditions.checkNotNull(
            cfa.getFunctionHead(functionName), "Function '" + functionName + "' was not found.");

    CallstackState initialStack =
        (CallstackState)
            callstackCPA.getInitialState(
                functioncallNode, StateSpacePartition.getDefaultPartition());
    LocationState initialLoc =
        locationCPA.getInitialState(functioncallNode, StateSpacePartition.getDefaultPartition());

    return old.addThread(threadId, old.nextActiveThread(), initialLoc, initialStack);
  }
}
