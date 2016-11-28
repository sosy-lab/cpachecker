/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.summary.summaryUsage;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.ToComputeSummaryState;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.UseSummaryCPA;
import org.sosy_lab.cpachecker.cpa.summary.summaryGeneration.SummaryState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Guiding the summary computation.
 *
 * Operates either over:
 * <ul>
 *   <li>Wrapped states directly</li>
 *   <li>{@link ToComputeSummaryState} to signal the need for recomputation.</li>
 * </ul>
 */
public class TopLevelSummaryCPA implements UseSummaryCPA,
                                           PrecisionAdjustment,
                                           AbstractDomain,
                                           TransferRelation,
                                           SummaryManager {
  private final UseSummaryCPA wrapped;

  /**
   * ReachedSet of {@link org.sosy_lab.cpachecker.cpa.summary.summaryGeneration.SummaryState}
   * essentially, a mapping from functions to summaries.
   */
  private final ReachedSet summaryMapping;

  public TopLevelSummaryCPA(

      ConfigurableProgramAnalysis pWrapped,

      // todo: inject via the custom factory?..
      ReachedSet pSummaryMapping) {
    summaryMapping = pSummaryMapping;
    Preconditions.checkArgument(pWrapped instanceof UseSummaryCPA,
        "Top-level CPA for summary computation has to implement UseSummaryCPA.");
    wrapped = (UseSummaryCPA) pWrapped;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException, InterruptedException {

    try {
      return getAbstractSuccessors0(state, precision);
    } catch (CPAException pE) {
      throw new CPATransferException("Exception occurred", pE);
    }
  }

  private Collection<? extends AbstractState> getAbstractSuccessors0(
      AbstractState state, Precision precision) throws CPAException, InterruptedException {

    AbstractStateWithLocations locState = AbstractStates.extractStateByType(state,
        AbstractStateWithLocations.class);
    assert locState != null;

    List<AbstractState> toReturn = new ArrayList<>(1);

    for (CFAEdge edge : locState.getOutgoingEdges()) {

      if (edge.getPredecessor() instanceof FunctionEntryNode) {
        toReturn.addAll(functionCallPostcondition(
            (FunctionEntryNode) edge.getPredecessor(), precision, state));
      } else if (edge.getPredecessor() instanceof FunctionExitNode) {

        // todo: again consider dynamic inlinement.
        continue;
      } else {

        // Simply delegate to the wrapped CPA.
        toReturn.addAll(wrapped.getTransferRelation().getAbstractSuccessors(
            state, precision
        ));
      }
    }

    return toReturn;
  }

  private List<AbstractState> functionCallPostcondition(
      FunctionEntryNode pEntryNode,
      Precision pPrecision,
      AbstractState callsite
  ) throws CPAException, InterruptedException {
    String functionName = pEntryNode.getFunctionName();

    Collection<AbstractState> summaries =

        // todo: creating a temporary state is an ugly hack.
        summaryMapping.getReached(SummaryState.emptySummaryForFunction(functionName));

    List<AbstractState> toReturn = new ArrayList<>(1);

    for (AbstractState summary : summaries) {

      SummaryState pSummary = (SummaryState) summary;

      AbstractState projection = getSummaryManager().projectToPrecondition(pSummary);
      if (getAbstractDomain().isLessOrEqual(callsite, projection)) {
        toReturn.addAll(
            getAbstractSuccessorsForSummary(callsite, pPrecision, pSummary)
        );
      }
    }

    if (toReturn.isEmpty()) {

      // Communicate the desire to recompute the summary.
      return Collections.singletonList(new ToComputeSummaryState(callsite, pPrecision, pEntryNode));
    }
    return toReturn;

  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {
    if (state instanceof ToComputeSummaryState) {

      // Requesting summary recomputation.
      return Optional.of(
          PrecisionAdjustmentResult.create(
              state, precision, Action.BREAK
          )
      );
    }

    // todo: anything else?
    return Optional.of(
        PrecisionAdjustmentResult.create(state, precision, Action.CONTINUE)
    );
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForSummary(
      AbstractState state, Precision precision, Summary pSummary)
      throws CPATransferException, InterruptedException {
    return wrapped.getSummaryManager().getAbstractSuccessorsForSummary(
        state, precision, pSummary
    );
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return wrapped.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(this);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return this;
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    // N.B.: "main" is *not* treated as a function call.
    return wrapped.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return wrapped.getInitialPrecision(node, partition);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractState projectToPrecondition(Summary pSummary) {
    return wrapped.getSummaryManager().projectToPrecondition(pSummary);
  }

  @Override
  public AbstractState projectToPostcondition(Summary pSummary) {
    return wrapped.getSummaryManager().projectToPostcondition(pSummary);
  }

  @Override
  public Summary generateSummary(ReachedSet pReached) {
    return wrapped.getSummaryManager().generateSummary(pReached);
  }

  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    return wrapped.getAbstractDomain().join(state1, state2);
  }

  @Override
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    if (state1 instanceof ToComputeSummaryState
        || state2 instanceof ToComputeSummaryState) {
      return false;
    }
    return wrapped.getAbstractDomain().isLessOrEqual(state1, state2);
  }

  @Override
  public SummaryManager getSummaryManager() {
    return this;
  }
}
