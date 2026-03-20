// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.BasicBlockAggregator;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class PORTransferRelation implements TransferRelation {
  private final LocationCPA locationCPA;
  private final CallstackCPA callstackCPA;
  private final TransferRelation wrappedTransferRelation;

  private final CFA cfa;

  private final boolean aggregateBasicBlocks;
  private final BasicBlockAggregator basicBlockAggregator;

  public PORTransferRelation(
      ConfigurableProgramAnalysis wrappedCpa,
      Configuration pConfig,
      CFA pCfa,
      boolean pAggregateBasicBlocks,
      LogManager pLogger)
      throws InvalidConfigurationException {
    wrappedTransferRelation = wrappedCpa.getTransferRelation();
    locationCPA = LocationCPA.create(pCfa, pConfig);
    callstackCPA = new CallstackCPA(pConfig, pLogger);

    cfa = pCfa;

    aggregateBasicBlocks = pAggregateBasicBlocks;
    basicBlockAggregator = new SingleGlobalStatementBlockAggregator(pCfa);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision)
      throws CPATransferException, InterruptedException {
    if (!(state instanceof PORState porState)) {
      throw new CPATransferException("State is not a PORState.");
    }
    if (!(precision instanceof PORPrecision porPrecision)) {
      throw new CPATransferException("Precision is not PORPrecision");
    }

    Collection<CFAEdge> sourceSet = porState.getSourceSet(porPrecision);
    ArrayList<AbstractState> allSuccessors = new ArrayList<>();
    for (CFAEdge edge : sourceSet) {
      allSuccessors.addAll(getAbstractSuccessorsForEdge(state, precision, edge));
    }
    return allSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (!(state instanceof PORState porState)) {
      throw new CPATransferException("State is not a PORState.");
    }
    if (!(precision instanceof PORPrecision porPrecision)) {
      throw new CPATransferException("Precision is not PORPrecision");
    }

    // Determine which thread this edge belongs to (populated by getOutgoingEdges)
    final Integer pid = porState.getEdgePid(cfaEdge);
    if (pid == null) {
      throw new CPATransferException("Could not determine thread for edge " + cfaEdge);
    }

    if (aggregateBasicBlocks) {
      final CFANode startNode = cfaEdge.getPredecessor();

      // dynamic multiEdges may be used if the following conditions apply
      if (basicBlockAggregator.isValidMultiEdgeStart(startNode)
          && basicBlockAggregator.isValidMultiEdgeComponent(startNode, cfaEdge)) {

        Collection<PORState> currentStates = new ArrayList<>(1);
        currentStates.add(porState);

        while (basicBlockAggregator.isValidMultiEdgeComponent(startNode, cfaEdge)) {
          Collection<PORState> successorStates = new ArrayList<>(currentStates.size());

          for (PORState currentState : currentStates) {
            getAbstractSuccessorsForEdge(currentState, porPrecision, cfaEdge, pid, successorStates);
          }

          // if there are no successors for the current edge, we do not need to continue
          if (successorStates.isEmpty()) {
            return ImmutableList.of();
          }

          // if we found a target state in the current successors immediately return
          if (from(successorStates).anyMatch(AbstractStates::isTargetState)) {
            return successorStates;
          }

          // make successor states the new to-be-handled states for the next edge
          currentStates = Collections.unmodifiableCollection(successorStates);

          // if there is more than one leaving edge we do not create a further multi edge part
          if (cfaEdge.getSuccessor().getNumLeavingEdges() == 1) {
            // all current states should be the same PORState
            cfaEdge = currentStates.iterator().next().getNextBasicBlockEdge(pid);
          } else {
            break;
          }
        }

        return currentStates;
      }
    }

    Collection<PORState> results = new ArrayList<>(1);
    getAbstractSuccessorsForEdge(porState, porPrecision, cfaEdge, pid, results);
    return results;
  }

  private void getAbstractSuccessorsForEdge(
      PORState state,
      PORPrecision precision,
      CFAEdge cfaEdge,
      int pid,
      Collection<PORState> result) throws CPATransferException, InterruptedException {
    // Call wrapped CPA transfer relation
    Collection<? extends AbstractState> wrappedSuccessors =
        wrappedTransferRelation.getAbstractSuccessorsForEdge(
            state.getWrappedState(),
            precision.getWrappedPrecision(),
            cfaEdge
        );

    if (wrappedSuccessors.isEmpty()) {
      return;
    }

    PORState prevState = state;

    if (cfaEdge instanceof AStatementEdge statementEdge) {
      AStatement statement = statementEdge.getStatement();
      if (statement instanceof AFunctionCall pAFunctionCall) {
        AExpression functionNameExp =
            pAFunctionCall.getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression pFunctionName) {
          final String functionName = pFunctionName.getName();
          final var params =
              pAFunctionCall.getFunctionCallExpression().getParameterExpressions();

          if (ThreadFunctions.isCreateFunction(functionName)) {
            String handle = ThreadFunctions.extractCreateHandle(params);
            String threadFunc = ThreadFunctions.extractCreateFunctionName(params);
            prevState = addNewThread(prevState, handle, threadFunc);

          } else if (ThreadFunctions.isJoinFunction(functionName)) {
            String handle = ThreadFunctions.extractJoinHandle(params);
            prevState = prevState.joinThread(handle);
            if (prevState == null) {
              return;
            }
          } else if (ThreadFunctions.isThreadExitFunction(functionName)) {
            prevState = prevState.exitThread(pid, locationCPA.getStateFactory());
            if (prevState == null) {
              return;
            }
          }
        }
      }
    }

    final PORState old = prevState;
    final PORThreadState threadState = old.threads().get(pid);

    if (threadState != null) {
      final var loc = threadState.pLocationState();
      final var stack = threadState.pCallstackState();

      final var nextLocs =
          locationCPA
              .getTransferRelation()
              .getAbstractSuccessorsForEdge(loc, precision, cfaEdge);
      final var nextStacks =
          callstackCPA
              .getTransferRelation()
              .getAbstractSuccessorsForEdge(stack, precision, cfaEdge);

      List<PORState> successors =
          nextLocs.stream()
              .flatMap(
                  nextLoc ->
                      nextStacks.stream()
                          .map(
                              nextStack ->
                                  old.stepThread(
                                      pid,
                                      (LocationState) nextLoc,
                                      (CallstackState) nextStack)))
              .toList();

      // Combine POR successors with wrapped CPA successors
      for (PORState porSuccessor : successors) {
        for (AbstractState wrappedSuccessor : wrappedSuccessors) {
          result.add(porSuccessor.withWrappedState(wrappedSuccessor));
        }
      }

      return;
    }

    throw new CPATransferException("Thread state not found for PID " + pid);
  }

  PORState initial(AbstractState wrappedInitialState) {
    return addNewThread(PORState.empty(wrappedInitialState, cfa), null, "main");
  }

  PORState addNewThread(final PORState old, final String handle, final String functionName) {
    CFANode functionCallNode =
        Preconditions.checkNotNull(
            cfa.getFunctionHead(functionName), "Function '%s' was not found.", functionName);

    CallstackState initialStack =
        (CallstackState)
            callstackCPA.getInitialState(
                functionCallNode, StateSpacePartition.getDefaultPartition());
    LocationState initialLoc =
        locationCPA.getInitialState(functionCallNode, StateSpacePartition.getDefaultPartition());

    return old.addNewThread(handle, initialLoc, initialStack);
  }
}
