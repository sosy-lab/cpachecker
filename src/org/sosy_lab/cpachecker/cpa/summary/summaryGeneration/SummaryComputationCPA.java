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
import com.google.common.collect.Iterables;
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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.cpa.summary.blocks.BlockManager;
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

  // todo: this flag is ugly
  @Option(secure=true, description="Whether summaries should be merged")
  private boolean joinSummaries = false;

  private final TopLevelSummaryCPA wrapped;

  private final CPAAlgorithmFactory algorithmFactory;
  private final ReachedSetFactory reachedSetFactory;
  private final BlockManager blockManager;
  private final SummaryManager wrappedSummaryManager;
  private final StopOperator wrappedStopOperator;
  private final MergeOperator wrappedMergeOperator;
  private final SummaryComputationStatistics statistics;
  private final LogManager logger;
  private final CFA cfa;

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
      ConfigurableProgramAnalysis pWrapped)
      throws InvalidConfigurationException, CPATransferException {
    super(pWrapped);
    pConfig.inject(this);
    cfa = pCfa;
    reachedSetFactory = pReachedSetFactory;
    Preconditions.checkArgument(pWrapped instanceof UseSummaryCPA,
        "Parameter CPA "
            + pWrapped.getClass().toString()
            + " has to implement the SummaryCPA interface.");

    computedSummaries = LinkedHashMultimap.create();
    logger = pLogger;
    statistics = new SummaryComputationStatistics();
    blockManager = new BlockManager(pCfa, pConfig, pLogger);
    wrapped = new TopLevelSummaryCPA(
        pWrapped, computedSummaries, blockManager, pLogger);
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
        blockManager.getBlockForNode(node),
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
    logger.log(Level.INFO, "Processing computation request for '",
        functionName, "', current reachedSet size is " + reached.size());

    CPAAlgorithm algorithm = algorithmFactory.newInstance();

    // todo: check the algorithm status. What about termination? That can be used as well.
    AlgorithmStatus status = algorithm.run(reached);
    assert status.isSound();

    List<AbstractState> toReEnqueue = new ArrayList<>();

    // Requests for summary computation is not empty:
    // put the new requests into the priority queue,
    // as well as the updated request for the currently processed function.
    List<SummaryComputationState> toReturn = new ArrayList<>();
    for (SummaryComputationRequest req : Iterables.consumingIterable(
                                  wrapped.getSummaryComputationRequests())) {
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
          ImmutableSet.of(),
          summaryComputationState);

      // Make sure that we restart the computation at those states,
      // now with summaries already generated.
      toReEnqueue.add(req.getCallingContext());

      logger.log(Level.INFO,
          "Intraprocedural requested for function ", scs.getFunctionName()
            + " with entry state ", req.getFunctionEntryState());
      toReturn.add(scs);
    }

    // Re-add the summary computation state to the reached set:
    // we want to compute the summary for that one.
    // Important: *relies* on the correct computation order,
    // would get stuck in the infinite loop otherwise.
    if (!toReturn.isEmpty()) {

      toReturn.add(summaryComputationState.withUnfinished());
      logger.log(Level.INFO, "Re-requesting intraprocedural for '",
          summaryComputationState.getFunctionName(), "'");
    }


    // We assume the wrapped state is an ARGState.
    ARGState lastState = (ARGState) reached.getLastState();

    // Early termination in case an error was found.
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
      returnStates = AbstractStates.filterLocation(
          reached, summaryComputationState.getBlock().getExitNode()
      ).filter(ARGState.class).filter(s -> s.getChildren().isEmpty()).toList();
    }


    if (hasTargetState || hasWaitingState) {

      logger.log(Level.INFO, "Has target state = " + hasTargetState,
          " has waiting state = " + hasWaitingState + " returning same state");
      return Collections.singleton(summaryComputationState.withUpdatedTargetable(
              hasWaitingState, hasTargetState, pViolatedProperties));
    }

    toReEnqueue.forEach(e -> reached.reAddToWaitlist(e));

    // Generate the summaries if we're not in the outer function,
    // and there was a way to a "return" node.
    Collection<? extends Summary> generatedSummaries;
    if (!returnStates.isEmpty() &&
        !summaryComputationState.getBlock().getFunctionName().equals(
            cfa.getMainFunction().getFunctionName())) {

      generatedSummaries = wrappedSummaryManager.generateSummaries(
          summaryComputationState.getCallingContext().get(),
          summaryComputationState.getEntryPrecision(),
          returnStates,
          returnStates.stream().map(
              s -> reached.getPrecision(s)
          ).collect(Collectors.toList()),
          summaryComputationState.getEntryLocation(),
          summaryComputationState.getBlock()
      );
      logger.log(Level.INFO, "Generated summaries: ", generatedSummaries);

    } else {

      // Otherwise, assume "unreachable".
      generatedSummaries = Collections.emptyList();
    }

    boolean changed = false;

    for (Summary s : generatedSummaries) {
      changed |= storeGeneratedSummary(s, functionName);
//      storeGeneratedSummary(s, functionName);
    }

//    // todo: if the generated summary
//    if (!changed) {
//
//      // The summary was subsumed by the existing ones.
//      return toReturn;
//    }

    return toReturn;
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

      if (wrappedSummaryManager.isDescribedBy(generatedSummary, existingSummary)) {

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
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return false;
//    SummaryComputationState sState1 = (SummaryComputationState) state1;
//    SummaryComputationState sState2 = (SummaryComputationState) state2;
//
//    // todo: probably would have to duplicate some
//    // of the logic from ARGStopSep class,
//    // especially if we want to start visualizing the dependencies between summaries.
//
//    if (sState1.isTarget()) {
//
//      // Do not cover target states.
//      return false;
//    }
//
//    if (!sState1.isFinished() || !sState2.isFinished()) {
//      // Do not cover unfinished computations.
//      return false;
//    }
//
//    // todo: what if we are re-adding the same state?
//    // that should actually be uncovered.
//    // or should we force that to be uncovered?
//    // they *would* have the same entry state!!
//
//    if (sState1.getEntryState() == sState2.getEntryState()) {
//
//      // todo: not 100% sure it makes sense....
//      return true;
//    }
//
//    // todo: for now don't do extra coverage...
//    // actually, the statement below is merely an optimization.
//    // hmm if we handle the states with deeper callstack
//    // first that *might* solve our problem.
//    if (joinSummaries
//        && wrapped.getAbstractDomain().isLessOrEqual(
//            sState1.getEntryState(), sState2.getEntryState())) {
//
//      // Stronger precondition: already subsumed by the existing summary.
//      return true;
//    }
//
//    return false;
  }

  @Override
  public AbstractState merge(
      AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {

    return state2;

//
//    // todo: check coverage as well?
//
//    SummaryComputationState sState1 = (SummaryComputationState) state1;
//    SummaryComputationState sState2 = (SummaryComputationState) state2;
//
//    if (!sState2.isFinished() || !sState1.isFinished()) {
//      return sState2;
//    }
//
//    Preconditions.checkArgument(sState1.getBlock() == sState2.getBlock());
//    Preconditions.checkState(!sState1.hasWaitingState() && !sState2.hasWaitingState()
//      && !sState1.isTarget() && !sState2.isTarget());
//
//    if (!sState1.getCallingContext().isPresent()) {
//      Preconditions.checkState(!sState2.getCallingContext().isPresent());
//
//      // There should always be at most one summary computation request for the
//      // main entry function.
//      // todo: not sure it's the case, we should be able to pick up and continue the analysis
//      // from "main".
//      return sState1;
//    }
//
//    if (joinSummaries) {
//      AbstractState mergedEntryState = wrappedMergeOperator.merge(
//          sState1.getEntryState(),
//          sState2.getEntryState(),
//          precision
//      );
//
//      if (mergedEntryState == sState1.getEntryState()) {
//        return state1;
//      } else if (mergedEntryState == sState2.getEntryState()) {
//        return state2;
//      } else {
//
//        ReachedSet reached = reachedSetFactory.create();
//        reached.add(mergedEntryState, precision);
//
//        return SummaryComputationState.of(
//            sState1.getBlock(),
//            wrappedMergeOperator.merge(
//                sState1.getCallingContext().get(), sState2.getCallingContext().get(), precision
//            ),
//            mergedEntryState,
//            precision,
//            reached,
//            false,
//            false,
//            ImmutableSet.of());
//      }
//    } else {
//      return state2;
//    }
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
