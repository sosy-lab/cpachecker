// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsTransferRelation;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicValueAssigner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/** Strongest post-operator based on symbolic value analysis. */
public class ValueTransferBasedStrongestPostOperator
    implements SymbolicStrongestPostOperator, StatisticsProvider {

  private final ValueAnalysisTransferRelation valueTransfer;
  // used for abstraction
  private final ValueAnalysisStrongestPostOperator valueStrongestPost;
  private final ConstraintsTransferRelation constraintsTransfer;

  private final ConstraintsStatistics constraintsStatistics;

  public ValueTransferBasedStrongestPostOperator(
      final ConstraintsSolver pSolver,
      final LogManager pLogger,
      final Configuration pConfig,
      final CFA pCfa)
      throws InvalidConfigurationException {

    valueTransfer =
        new ValueAnalysisTransferRelation(
            pLogger,
            pCfa,
            new ValueAnalysisTransferRelation.ValueTransferOptions(pConfig),
            new SymbolicValueAssigner(pConfig),
            new ConstraintsStrengthenOperator(pConfig, pLogger),
            null);

    valueStrongestPost = new ValueAnalysisStrongestPostOperator(pLogger, pConfig, pCfa);

    // Use name of this strongest post operator to differentiate from ConstraintsCPA
    constraintsStatistics =
        new ConstraintsStatistics(ValueTransferBasedStrongestPostOperator.class.getSimpleName());

    constraintsTransfer =
        new ConstraintsTransferRelation(
            pSolver, constraintsStatistics, pCfa.getMachineModel(), pLogger, pConfig);
  }

  @Override
  public Optional<ForgettingCompositeState> getStrongestPost(
      final ForgettingCompositeState pOrigin, final Precision pPrecision, final CFAEdge pOperation)
      throws CPAException, InterruptedException {

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
              oldConstraints, SingletonPrecision.getInstance(), pOperation);

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
            getNewCompositeState(
                valueStrengthenResult.orElseThrow(), constraintsStrengthenResult.orElseThrow()));
      }
    }
  }

  @Override
  public ForgettingCompositeState handleFunctionCall(
      final ForgettingCompositeState pStateBeforeCall,
      final CFAEdge pEdge,
      final Deque<ForgettingCompositeState> pCallstack) {
    pCallstack.push(pStateBeforeCall);
    return pStateBeforeCall;
  }

  @Override
  public ForgettingCompositeState handleFunctionReturn(
      final ForgettingCompositeState pNext,
      final CFAEdge pEdge,
      final Deque<ForgettingCompositeState> pCallstack) {
    final ForgettingCompositeState callState = pCallstack.pop();

    // Do not forget any information about constraints.
    // In constraints, IdExpressions are already resolved to symbolic expression and as such
    // independent of scope.
    final ConstraintsState constraintsState = pNext.getConstraintsState();

    ValueAnalysisState currentValueState = pNext.getValueState();
    ValueAnalysisState callStateValueState = callState.getValueState();

    currentValueState =
        currentValueState.rebuildStateAfterFunctionCall(
            callStateValueState, (FunctionExitNode) pEdge.getPredecessor());

    return getNewCompositeState(currentValueState, constraintsState);
  }

  @Override
  public ForgettingCompositeState performAbstraction(
      final ForgettingCompositeState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision) {
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
      final CFAEdge pOperation)
      throws CPATransferException {

    Collection<? extends AbstractState> strengthenResult =
        valueTransfer.strengthen(pValues, ImmutableList.of(pConstraints), pOperation, pPrecision);

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
      final CFAEdge pOperation)
      throws CPATransferException, InterruptedException {

    Collection<? extends AbstractState> successors =
        constraintsTransfer.strengthen(
            pConstraintsState,
            ImmutableList.of(pValueState),
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

  private ForgettingCompositeState getNewCompositeState(
      final ValueAnalysisState pNextValueState, final ConstraintsState pConstraints) {

    return new ForgettingCompositeState(pNextValueState, pConstraints);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(constraintsStatistics);
  }
}
