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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.blocks.BlockManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.UseSummaryCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Guiding the summary computation.
 *
 * <p>Operates over wrapped states directly.
 * Reached set may contain an instance of {@link SummaryComputationRequest},
 * as a last state in a reached set, as after adding such a state a BREAK
 * action is issued by the precision adjustment.
 */
public class TopLevelSummaryCPA implements ConfigurableProgramAnalysis,
                                           PrecisionAdjustment,
                                           AbstractDomain,
                                           TransferRelation,
                                           StatisticsProvider {

  private final UseSummaryCPA wrapped;
  private final Multimap<String, Summary> summaryMapping;
  private final SummaryManager wrappedSummaryManager;
  private final AbstractDomain wrappedAbstractDomain;
  private final TransferRelation wrappedTransferRelation;
  private final PrecisionAdjustment wrappedPrecisionAdjustment;
  private final LogManager logger;
  private final MergeOperator wrappedMergeOperator;
  private final StopOperator wrappedStopOperator;
  private final List<SummaryComputationRequest> summaryComputationRequests;
  private final BlockManager blockManager;

  public TopLevelSummaryCPA(
      ConfigurableProgramAnalysis pWrapped,
      Multimap<String, Summary> pSummaryMapping,
      BlockManager pBlockManager,
      LogManager pLogger) throws InvalidConfigurationException, CPATransferException {
    summaryMapping = pSummaryMapping;
    Preconditions.checkArgument(pWrapped instanceof UseSummaryCPA,
        "Top-level CPA for summary computation has to implement UseSummaryCPA.");
    wrapped = (UseSummaryCPA) pWrapped;
    wrappedSummaryManager = wrapped.getSummaryManager();
    wrappedAbstractDomain = wrapped.getAbstractDomain();
    wrappedTransferRelation = wrapped.getTransferRelation();
    wrappedPrecisionAdjustment = wrapped.getPrecisionAdjustment();
    wrappedMergeOperator = wrapped.getMergeOperator();
    wrappedStopOperator = wrapped.getStopOperator();
    logger = pLogger;
    summaryComputationRequests = new ArrayList<>();
    blockManager = pBlockManager;
  }

  public List<SummaryComputationRequest> getSummaryComputationRequests() {
    return summaryComputationRequests;
  }

  public void clearComputationRequests() {
    summaryComputationRequests.clear();
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

    CFANode node = AbstractStates.extractLocation(state);
    Block block = blockManager.getBlockForNode(node);

    if (block.getExitNode() == node) {

      // Analysis only goes up to the block end.
      return Collections.emptyList();
    } else if (
        node.getNumLeavingEdges() == 1 &&
            blockManager.getBlockForNode(node.getLeavingEdge(0).getSuccessor()) != block
        ) {

      logger.log(Level.INFO, "IDENTIFIED CALL NODE", node);

      // todo: avoid repetition
      Block calledBlock = blockManager.getBlockForNode(
          node.getLeavingEdge(0).getSuccessor()
      );

      // Attempt to calculate a postcondition using summaries
      // we have.
      // If our summaries are not sufficient, request a generation of a new one.
      return applySummaries(
          node,
          precision,
          state,
          calledBlock
      );
    } else {

      // Simply delegate.
      return wrappedTransferRelation.getAbstractSuccessors(state, precision);
    }
  }

  private Collection<? extends AbstractState> applySummaries(
      CFANode pCallNode,
      Precision pPrecision,
      AbstractState pCallsite,
      Block pBlock
  ) throws CPAException, InterruptedException {
    String functionName = pCallNode.getFunctionName();

    List<Summary> summaries = ImmutableList.copyOf(summaryMapping.get(functionName));
    List<Summary> matchingSummaries = new ArrayList<>();

    // We can return multiple postconditions, one for each matching summary.
    for (Summary summary : summaries) {
      AbstractState projection = wrappedSummaryManager.projectToCallsite(summary);
      if (wrappedAbstractDomain.isLessOrEqual(pCallsite, projection)) {
        matchingSummaries.add(summary);
      }
    }

    if (matchingSummaries.isEmpty()) {
      logger.log(Level.INFO, "No matching summary found for ",
          pCallNode.getFunctionName(), ", requesting recomputation");

      AbstractState weakenedCallState = wrappedSummaryManager.getWeakenedCallState(
          pCallsite, pPrecision, pBlock
      );

      Collection<? extends AbstractState> entryState =
          wrappedTransferRelation.getAbstractSuccessors(weakenedCallState, pPrecision);

      logger.log(Level.INFO, "Generated entry state to the function ",
          AbstractStates.extractLocation(entryState.iterator().next()),
          " starting from the call node", AbstractStates.extractLocation(pCallsite));

      // todo: would be nice to remove this assumption.
      Preconditions.checkState(entryState.size() == 1,
          "Processing function call edge should create a unique successor");

      // Communicate the desire to recompute the summary.
      summaryComputationRequests.add(new SummaryComputationRequest(
          pCallsite,
          entryState.iterator().next(),
          pPrecision,
          pBlock));
    }

    if (summaries.isEmpty()) {

      // No summaries at all is equivalent to having a "bottom" summary.
      // Hence no successors are returned.
      return Collections.emptyList();
    }

    return Collections.singleton(wrappedSummaryManager.getAbstractSuccessorForSummary(
        pCallsite, pPrecision, summaries, pBlock
    ));
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {

   return wrappedPrecisionAdjustment.prec(
        state, precision, states, stateProjection, fullState);
  }


  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return wrappedMergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return wrappedStopOperator;
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
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    return wrappedAbstractDomain.isLessOrEqual(state1, state2);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (wrapped instanceof StatisticsProvider) {
      ((StatisticsProvider) wrapped).collectStatistics(pStatsCollection);
    }
  }

  public SummaryManager getSummaryManager() {
    return wrappedSummaryManager;
  }

  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("Unexpected API call");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("Unexpected API Call");
  }
}
