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
package org.sosy_lab.cpachecker.cpa.summary.summaryGeneration;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.ExtendedBlockPartitioningBuilder;
import org.sosy_lab.cpachecker.cfa.blocks.builder.FunctionPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.PartitioningHeuristic;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.UseSummaryCPA;
import org.sosy_lab.cpachecker.cpa.summary.summaryUsage.SummaryComputationRequest;
import org.sosy_lab.cpachecker.cpa.summary.summaryUsage.TopLevelSummaryCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Top-level CPA for summary generation.
 *
 * Performs computations over {@link SummaryComputationState},
 * which represents (partial) computation of a summary.
 * The full computation does not have successors, and is added
 * to a stateful datastructure for storing summaries.
 *
 */
@Options(prefix="cpa.summary")
public class SummaryComputationCPA
    extends AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysis,
               AbstractDomain,
               TransferRelation,
               PrecisionAdjustment,
               MergeOperator,
               StatisticsProvider {

  @Option(secure=true, description="Whether summaries should be merged")
  private boolean joinSummaries = false;

  @Option(
      secure = true,
      description = "Factory for the class which partitions the CFA "
          + "into blocks."
  )
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.cfa.blocks.builder")
  private PartitioningHeuristic.Factory partitioningHeuristicFactory = FunctionPartitioning::new;

  private final TopLevelSummaryCPA wrapped;

  private final CPAAlgorithmFactory algorithmFactory;
  private final ReachedSetFactory reachedSetFactory;
  private final BlockPartitioning blockPartitioning;
  private final SummaryManager wrappedSummaryManager;
  private final StopOperator wrappedStopOperator;
  private final MergeOperator wrappedMergeOperator;
  private final SummaryComputationStatistics statistics;
  private final LogManager logger;

  /**
   * Function name to stored summaries mapping.
   * Order-preserving multimap.
   *
   * <p>Maintains an invariant that no stored summary strictly
   * subsumes the other one.
   */
  private final Multimap<String, Summary> computedSummaries;

  public SummaryComputationCPA(
      LogManager pLogger,
      Configuration pConfig,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pWrapped) throws InvalidConfigurationException {
    super(pWrapped);
    pConfig.inject(this);
    reachedSetFactory = pReachedSetFactory;
    Preconditions.checkArgument(pWrapped instanceof UseSummaryCPA,
        "Parameter CPA "
            + pWrapped.getClass().toString()
            + " has to implement the SummaryCPA interface.");

    computedSummaries = LinkedHashMultimap.create();
    PartitioningHeuristic heuristic = partitioningHeuristicFactory.create(
        pLogger, pCfa, pConfig
    );
    blockPartitioning = heuristic.buildPartitioning(pCfa, new ExtendedBlockPartitioningBuilder());
    logger = pLogger;
    statistics = new SummaryComputationStatistics();
    wrapped = new TopLevelSummaryCPA(
        pWrapped, computedSummaries, blockPartitioning, pLogger);
    wrappedSummaryManager = wrapped.getSummaryManager();
    wrappedMergeOperator = wrapped.getMergeOperator();
    wrappedStopOperator = wrapped.getStopOperator();

    algorithmFactory = new CPAAlgorithmFactory(
        wrapped, pLogger, pConfig, pShutdownNotifier
    );
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition)
        throws InterruptedException {
    ReachedSet reached = reachedSetFactory.create();
    AbstractState entryState = wrapped.getInitialState(node, partition);
    Precision entryPrecision = wrapped.getInitialPrecision(node, partition);
    reached.add(entryState, entryPrecision);

    return SummaryComputationState.initial(
        blockPartitioning.getMainBlock(),
        entryState,
        entryPrecision,
        reached);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException,
                                                       InterruptedException {
    try {
      return getAbstractSuccessors0((SummaryComputationState) state);
    } catch (CPAException pE) {
      throw new CPATransferException("Exception occurred", pE);
    }
  }

  private Collection<SummaryComputationState> getAbstractSuccessors0(
      SummaryComputationState summaryComputationState)
      throws CPAException, InterruptedException {

    String functionName = summaryComputationState.getFunctionName();
    ReachedSet reached = summaryComputationState.getReached();

    CPAAlgorithm algorithm = algorithmFactory.newInstance();

    logger.log(Level.INFO, "Requesting intraprocedural analysis for ",
        functionName, "; current reachedSet size is " + reached.size());

    // todo: check the algorithm status. What about termination?
    AlgorithmStatus status = algorithm.run(reached);
    assert status.isSound();

    // Requests for summary computation is not empty:
    // put the new requests into the priority queue,
    // as well as the updated request for the currently processed function.

    // todo: should we check that no summary has appeared in the meantime?

    // NB: it is important to make sure that the summary computation request
    // is expanded *before* the request for recomputing the summary
    // for "summaryComputationState".
    List<SummaryComputationState> toReturn = new ArrayList<>();
    for (SummaryComputationRequest req : wrapped.getSummaryComputationRequests()) {
      ReachedSet newReached = reachedSetFactory.create();
      newReached.add(req.getFunctionEntryState(), req.getFunctionEntryPrecision());
      SummaryComputationState scs = SummaryComputationState.of(
          req.getBlock(),
          req.getCallingContext(),
          req.getFunctionEntryState(),
          req.getFunctionEntryPrecision(),
          newReached,
          false,
          false,
          ImmutableSet.of());

      logger.log(Level.INFO, "Recursive computation requested for function " + scs.getFunctionName()
      + " with entry state " + req.getFunctionEntryState());
      toReturn.add(scs);
    }

    if (!wrapped.getSummaryComputationRequests().isEmpty()) {

      // Re-add the summary computation state to the reached set:
      // we want to compute the summary for that one.
      // Important: *relies* on the correct computation order,
      // would get stuck in the infinite loop otherwise.
      toReturn.add(summaryComputationState);
    }

    // We assume the wrapped state is an ARGState, unless it was a
    // computation request.
    ARGState lastState = (ARGState) reached.getLastState();

    boolean hasWaitingState = reached.hasWaitingState();
    final List<ARGState> returnStates;
    boolean hasTargetState = AbstractStates.isTargetState(lastState);
    Set<Property> pViolatedProperties = hasTargetState ?
                                        lastState.getViolatedProperties() :
                                        ImmutableSet.of();

    if (hasTargetState || hasWaitingState) {
      assert lastState != null;
      returnStates = ImmutableList.of(lastState);
    } else {
      returnStates = AbstractStates.filterLocations(
          reached, summaryComputationState.getBlock().getReturnNodes()
      ).filter(ARGState.class).filter(s -> s.getChildren().isEmpty()).toList();
    }

    List<Precision> returnPrecisions = returnStates.stream()
        .map(s -> reached.getPrecision(s))
        .collect(Collectors.toList());

    if (hasTargetState || hasWaitingState) {

      logger.log(Level.INFO, "Has target state = " + hasTargetState,
          " has waiting state = " + hasWaitingState + " returning same state");

      return Collections.singleton(summaryComputationState.withUpdatedTargetable(
              hasWaitingState, hasTargetState, pViolatedProperties));
    } else {

      if (summaryComputationState.getCallingContext().isPresent()) {

        boolean changed = false;

        // Generate the summaries if we're not in the outer function.
        // No new summaries were needed, can finally generate the summary for this function.
        Collection<? extends Summary> generatedSummaries = wrappedSummaryManager.generateSummaries(
            summaryComputationState.getCallingContext().get(),
            summaryComputationState.getEntryPrecision(),
            returnStates,
            returnPrecisions,
            summaryComputationState.getEntryLocation(),
            summaryComputationState.getBlock()
        );
        for (Summary s : generatedSummaries) {
          changed |= storeGeneratedSummary(s, functionName);
        }

        if (!changed) {

          // The summary was subsumed by the existing ones.
          return Collections.emptyList();
        }
      }

      // todo: can we have empty return from the main? what if "main" is recursive itself?..
      return toReturn;
    }
  }

  /**
   * Store the generated summary in the {@link #computedSummaries} datastructure.
   *
   * <p>Merges the summary with the existing ones,
   * and additionally perform the coverage check.
   * The logic duplicates that of {@link CPAAlgorithm}
   * for the merge and the subsequent coverage check.
   *
   * @param generatedSummary Summary which was generated.
   * @param functionName Name of the function for the generated summary.
   *
   * @return whether the collection was changed (if not, the summary was subsumed).
   */
  private boolean storeGeneratedSummary(
      Summary generatedSummary, String functionName)
      throws CPAException, InterruptedException {


    // Do the merge.
    Collection<Summary> matchingSummaries = computedSummaries.get(functionName);
    List<Summary> toRemove = new ArrayList<>();
    List<Summary> toAdd = new ArrayList<>();
    for (Summary existingSummary : matchingSummaries) {
      Summary merged = wrappedSummaryManager.merge(generatedSummary, existingSummary);
      if (merged != existingSummary) {
        toRemove.add(existingSummary);
        toAdd.add(merged);
      }
    }
    matchingSummaries.removeAll(toRemove);
    matchingSummaries.addAll(toAdd);

    // Do the coverage computation.
    boolean add = true;
    for (Summary existingSummary : matchingSummaries) {

      // todo: STOP operator should be used instead.
      // would actually make the API nicer as well.

      if (wrappedSummaryManager.isDescribedBy(
          generatedSummary, existingSummary, wrapped.getAbstractDomain())) {

        // Summary is subsumed, do nothing.
        add = false;
        break;
      }
    }
    if (add) {
      matchingSummaries.add(generatedSummary);

      // todo: what about the matching case?..
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    SummaryComputationState sState1 = (SummaryComputationState) state1;
    SummaryComputationState sState2 = (SummaryComputationState) state2;

    // todo: probably would have to duplicate some
    // of the logic from ARGStopSep class,
    // especially if we want to start visualizing the dependencies between summaries.

    if (sState1.isTarget()) {

      // Do not cover target states.
      return false;
    }

    if (sState1.getEntryState() == sState2.getEntryState()) {

      // todo: not 100% sure it makes sense....
      return true;
    }

    // todo: for now don't do extra coverage...
    // actually, the statement below is merely an optimization.
    // hmm if we handle the states with deeper callstack
    // first that *might* solve our problem.
    if (joinSummaries
        && wrapped.getAbstractDomain().isLessOrEqual(
            sState1.getEntryState(), sState2.getEntryState())) {

      // Stronger precondition: already subsumed by the existing summary.
      return true;
    }

    return false;
  }

  @Override
  public AbstractState merge(
      AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {

    // todo: check coverage as well?

    SummaryComputationState sState1 = (SummaryComputationState) state1;
    SummaryComputationState sState2 = (SummaryComputationState) state2;
    Preconditions.checkArgument(sState1.getBlock() == sState2.getBlock());
    Preconditions.checkState(!sState1.hasWaitingState() && !sState2.hasWaitingState()
      && !sState1.isTarget() && !sState2.isTarget());

    if (!sState1.getCallingContext().isPresent()) {
      Preconditions.checkState(!sState2.getCallingContext().isPresent());

      // There should always be at most one summary computation request for the
      // main entry function.
      return sState1;
    }

    if (joinSummaries) {
      AbstractState mergedEntryState = wrappedMergeOperator.merge(
          sState1.getEntryState(),
          sState2.getEntryState(),
          precision
      );

      // well isn't that fantastic, now we have two things to merge:
      // entry node and the calling context.

      if (mergedEntryState == sState2.getEntryState()) {
        return state2;
      } else {

        ReachedSet reached = reachedSetFactory.create();
        reached.add(mergedEntryState, precision);

        return SummaryComputationState.of(
            sState1.getBlock(),
            wrappedMergeOperator.merge(
                sState1.getCallingContext().get(), sState2.getCallingContext().get(), precision
            ),
            mergedEntryState,
            precision,
            reached,
            false,
            false,
            ImmutableSet.of());
      }
    } else {
      return state2;
    }
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {

    SummaryComputationState sState = (SummaryComputationState) state;

    // Break on target or waiting state present.
    Action action = sState.isTarget() || sState.hasWaitingState() ?
        Action.BREAK : Action.CONTINUE;

    return Optional.of(PrecisionAdjustmentResult.create(state, precision, action));
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this;
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
    return SingletonPrecision.getInstance();
  }


  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("Unexpected API call.");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("Unexpected API usage");
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SummaryComputationCPA.class);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
    wrapped.collectStatistics(statsCollection);
  }

}
