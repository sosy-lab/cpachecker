/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsTransferRelation;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import java.util.Collection;
import java.util.Deque;
import java.util.Optional;

/**
 * Strongest post-operator based on symbolic value analysis.
 */
public class ValueTransferBasedStrongestPostOperator
    implements SymbolicStrongestPostOperator {

  private final ValueAnalysisTransferRelation valueTransfer;
  // used for abstraction
  private final ValueAnalysisStrongestPostOperator valueStrongestPost;
  private final ConstraintsTransferRelation constraintsTransfer;

  public ValueTransferBasedStrongestPostOperator(
      final Solver pSolver,
      final LogManager pLogger,
      final Configuration pConfig,
      final CFA pCfa,
      final ShutdownNotifier pShutdownNotifier
  ) throws InvalidConfigurationException {


    valueTransfer =
        new ValueAnalysisTransferRelation(pConfig, pLogger, pCfa);

    valueStrongestPost = new ValueAnalysisStrongestPostOperator(pLogger, pConfig, pCfa);

    constraintsTransfer =
        new ConstraintsTransferRelation(pSolver,
                                        pCfa.getMachineModel(),
                                        pLogger,
                                        pConfig,
                                        pShutdownNotifier);
  }

  @Override
  public Optional<ForgettingCompositeState> getStrongestPost(
      final ForgettingCompositeState pOrigin,
      final Precision pPrecision,
      final CFAEdge pOperation
  ) throws CPAException, InterruptedException {

    ValueAnalysisState oldValues = pOrigin.getValueState();
    ConstraintsState oldConstraints = pOrigin.getConstraintsState();


    assert oldValues != null && oldConstraints != null;

    final Collection<ValueAnalysisState> successors =
        valueTransfer.getAbstractSuccessorsForEdge(oldValues, pPrecision, pOperation);

    if (isContradiction(successors)) {
      return Optional.empty();

    } else {
      final ValueAnalysisState valuesSuccessor = Iterables.getOnlyElement(successors);

      Collection<? extends AbstractState> constraintsSuccessors =
          constraintsTransfer.getAbstractSuccessorsForEdge(
              oldConstraints,
              SingletonPrecision.getInstance(),
              pOperation);

      if (isContradiction(constraintsSuccessors)) {
        return Optional.empty();
      }

      final ConstraintsState constraintsSuccessor =
          (ConstraintsState) Iterables.get(constraintsSuccessors, 0);

      Optional<ConstraintsState> constraintsStrengthenResult =
          strengthenConstraintsState(constraintsSuccessor, valuesSuccessor, pOperation);

      if (!constraintsStrengthenResult.isPresent()) {
        return Optional.empty();

      } else {
        Optional<ValueAnalysisState> valueStrengthenResult =
            strengthenValueState(valuesSuccessor, constraintsSuccessor, pPrecision, pOperation);

        if (!valueStrengthenResult.isPresent()) {
          return Optional.empty();
        }

        return Optional.of(
            getNewCompositeState(valueStrengthenResult.get(), constraintsStrengthenResult.get()));
      }
    }
  }

  @Override
  public ForgettingCompositeState handleFunctionCall(
      final ForgettingCompositeState pStateBeforeCall,
      final CFAEdge pEdge,
      final Deque<ForgettingCompositeState> pCallstack
  ) {
    pCallstack.push(pStateBeforeCall);
    return pStateBeforeCall;
  }

  @Override
  public ForgettingCompositeState handleFunctionReturn(
      final ForgettingCompositeState pNext,
      final CFAEdge pEdge,
      final Deque<ForgettingCompositeState> pCallstack
  ) {
    final ForgettingCompositeState callState = pCallstack.pop();

    // Do not forget any information about constraints.
    // In constraints, IdExpressions are already resolved to symbolic expression and as such
    // independent of scope.
    final ConstraintsState constraintsState = pNext.getConstraintsState();

    ValueAnalysisState currentValueState = pNext.getValueState();
    ValueAnalysisState callStateValueState = callState.getValueState();

    currentValueState = currentValueState.rebuildStateAfterFunctionCall(
            callStateValueState, (FunctionExitNode) pEdge.getPredecessor());

    return getNewCompositeState(currentValueState, constraintsState);
  }

  @Override
  public ForgettingCompositeState performAbstraction(
      final ForgettingCompositeState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision
  ) {
    ValueAnalysisState oldValueState = pNext.getValueState();

    assert pPrecision instanceof VariableTrackingPrecision;
    ValueAnalysisState newValueState =
        valueStrongestPost.performAbstraction(oldValueState, pCurrNode, pErrorPath, pPrecision);

    return getNewCompositeState(newValueState, pNext.getConstraintsState());
  }

  private Optional<ValueAnalysisState> strengthenValueState(
      final ValueAnalysisState pValues,
      final ConstraintsState pConstraints,
      final Precision pPrecision,
      final CFAEdge pOperation
  ) throws CPATransferException {

    Collection<? extends AbstractState> strengthenResult =
        valueTransfer.strengthen(pValues,
                                 ImmutableList.<AbstractState>of(pConstraints),
                                 pOperation,
                                 pPrecision);

    if (isContradiction(strengthenResult)) {
      return Optional.empty();

    } else {
      final AbstractState onlyState = Iterables.getOnlyElement(strengthenResult);

      return Optional.of((ValueAnalysisState) onlyState);
    }
  }


  private Optional<ConstraintsState> strengthenConstraintsState(
      final ConstraintsState pConstraintsState,
      final ValueAnalysisState pValueState,
      final CFAEdge pOperation
  ) throws CPATransferException, InterruptedException {

    Collection<? extends AbstractState> successors =
        constraintsTransfer.strengthen(pConstraintsState,
                                       ImmutableList.<AbstractState>of(pValueState),
                                       pOperation,
                                       SingletonPrecision.getInstance());

    if (isContradiction(successors)) {
      return Optional.empty();

    } else {
      final AbstractState onlyState = Iterables.getOnlyElement(successors);

      return Optional.of((ConstraintsState) onlyState);
    }
  }

  private boolean isContradiction(final Collection<? extends AbstractState> pAbstractStates) {
    return pAbstractStates.isEmpty();
  }

  private ForgettingCompositeState getNewCompositeState(final ValueAnalysisState pNextValueState,
      final ConstraintsState pConstraints) {

    return new ForgettingCompositeState(pNextValueState, pConstraints);
  }
}
