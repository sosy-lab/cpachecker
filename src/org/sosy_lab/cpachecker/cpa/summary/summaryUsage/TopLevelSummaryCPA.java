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
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.UseSummaryCPA;
import org.sosy_lab.cpachecker.cpa.summary.summaryGeneration.SummaryComputationStatistics;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Guiding the summary computation.
 *
 * <p>Operates over wrapped states directly.
 * Reached set may contain an instance of {@link SummaryComputationRequestState},
 * as a last state in a reached set, as after adding such a state a BREAK
 * action is issued by the precision adjustment.
 */
public class TopLevelSummaryCPA implements UseSummaryCPA,
               PrecisionAdjustment,
               AbstractDomain,
               TransferRelation,
               SummaryManager,
               StatisticsProvider {

  private final UseSummaryCPA wrapped;
  private final BlockPartitioning blockPartitioning;
  private final Multimap<String, Summary> summaryMapping;
  private final SummaryManager wrappedSummaryManager;
  private final AbstractDomain wrappedAbstractDomain;
  private final SummaryComputationStatistics statistics;
  private final TransferRelation wrappedTransferRelation;
  private final PrecisionAdjustment wrappedPrecisionAdjustment;
  private final LogManager logger;

  public TopLevelSummaryCPA(
      ConfigurableProgramAnalysis pWrapped,
      Multimap<String, Summary> pSummaryMapping,
      BlockPartitioning pBlockPartitioning,
      SummaryComputationStatistics pStatistics,
      LogManager pLogger) throws InvalidConfigurationException {
    summaryMapping = pSummaryMapping;
    Preconditions.checkArgument(pWrapped instanceof UseSummaryCPA,
        "Top-level CPA for summary computation has to implement UseSummaryCPA.");
    wrapped = (UseSummaryCPA) pWrapped;
    wrappedSummaryManager = wrapped.getSummaryManager();
    blockPartitioning = pBlockPartitioning;
    wrappedSummaryManager.setBlockPartitioning(blockPartitioning);
    wrappedAbstractDomain = wrapped.getAbstractDomain();
    wrappedTransferRelation = wrapped.getTransferRelation();
    wrappedPrecisionAdjustment = wrapped.getPrecisionAdjustment();
    statistics = pStatistics;
    logger = pLogger;
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return wrapped.getInitialState(node, partition);
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

    AbstractStateWithLocation locState = AbstractStates.extractStateByType(
        state, AbstractStateWithLocation.class);
    assert locState != null : "Wrapped state should have a unique location";

    CFANode node = locState.getLocationNode();
    if (blockPartitioning.isReturnNode(node)) {

      // Analysis only goes up to the block end.
      return Collections.emptyList();
    } else if (blockPartitioning.isCallNode(node)

        // Do not request a recursive computation on the first block in the program.
        && !(node.getNumEnteringEdges() == 0 && node.getEnteringSummaryEdge() == null)) {

      // Attempt to calculate a postcondition using summaries
      // we have.
      // If our summaries are not sufficient, request a generation of a new one.
      AbstractState out = applySummaries(
          node,
          precision,
          state,
          blockPartitioning.getBlockForCallNode(node)
      );

      return Collections.singleton(out);
    } else {

      // Simply delegate.
      return wrappedTransferRelation.getAbstractSuccessors(state, precision);
    }
  }

  private AbstractState applySummaries(
      CFANode pCallNode,
      Precision pPrecision,
      AbstractState pCallsite,
      Block pBlock
  ) throws CPAException, InterruptedException {
    String functionName = pCallNode.getFunctionName();

    Collection<Summary> summaries = summaryMapping.get(functionName);
    List<Summary> matchingSummaries = new ArrayList<>();

    // We can return multiple postconditions, one for each matching summary.
    for (Summary summary : summaries) {
      AbstractState projection = getSummaryManager().projectToCallsite(summary);
      if (getAbstractDomain().isLessOrEqual(pCallsite, projection)) {
        matchingSummaries.add(summary);
      }
    }

    if (matchingSummaries.isEmpty()) {
      logger.log(Level.INFO, "No matching summary found for ",
          pCallNode.getFunctionName(), ", requesting recomputation");

      Collection<? extends AbstractState> entryState =
          wrappedTransferRelation.getAbstractSuccessors(pCallsite, pPrecision);

      // todo: would be nice to remove this assumption.
      Preconditions.checkState(entryState.size() == 1,
          "Processing function call edge should create a unique successor");

      // Communicate the desire to recompute the summary.
      return new SummaryComputationRequestState(
          pCallsite,
          entryState.iterator().next(),
          pPrecision,
          pBlock);

    } else {
      logger.log(Level.INFO, "# ", matchingSummaries.size(), " matching summaries "
          + "found for " + pCallNode.getFunctionName());
      return wrappedSummaryManager.getAbstractSuccessorsForSummary(
          pCallsite, pPrecision, matchingSummaries, pBlock
      );
    }
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {

    if (state instanceof SummaryComputationRequestState) {
      return Optional.of(
          PrecisionAdjustmentResult.create(
              state, precision, Action.BREAK
          )
      );
    }

    return wrappedPrecisionAdjustment.prec(state, precision, states, stateProjection, fullState);
  }


  @Override
  public AbstractState getAbstractSuccessorsForSummary(
      AbstractState state, Precision precision, List<Summary> pSummary, Block pBlock)
      throws CPAException, InterruptedException {
    return wrappedSummaryManager.getAbstractSuccessorsForSummary(
        state, precision, pSummary, pBlock
    );
  }

  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pState, Precision pPrecision, Block pBlock) {
    return wrappedSummaryManager.getWeakenedCallState(
        pState, pPrecision, pBlock
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
  public AbstractState projectToCallsite(Summary pSummary) {
    return wrappedSummaryManager.projectToCallsite(pSummary);
  }

  @Override
  public AbstractState projectToPostcondition(Summary pSummary) {
    return wrappedSummaryManager.projectToPostcondition(pSummary);
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pEntryPrecision,
      List<? extends AbstractState> pReturnState,
      List<Precision> pReturnPrecision,
      CFANode pEntryNode,
      Block pBlock) {
    return wrappedSummaryManager.generateSummaries(
        pCallState, pEntryPrecision, pReturnState, pReturnPrecision, pEntryNode, pBlock);
  }

  @Override
  public Summary merge(
      Summary pSummary1, Summary pSummary2) throws CPAException, InterruptedException {
    return wrappedSummaryManager.merge(pSummary1, pSummary2);
  }

  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    return wrappedAbstractDomain.join(state1, state2);
  }

  @Override
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    return !(
        state1 instanceof SummaryComputationRequestState
        || state2 instanceof SummaryComputationRequestState)
        && wrappedAbstractDomain.isLessOrEqual(state1, state2);
  }

  @Override
  public SummaryManager getSummaryManager() {
    return this;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (wrapped instanceof StatisticsProvider) {
      ((StatisticsProvider) wrapped).collectStatistics(pStatsCollection);
    }
  }
}
