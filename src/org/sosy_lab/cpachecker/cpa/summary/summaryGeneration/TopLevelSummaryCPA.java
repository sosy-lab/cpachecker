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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cpa.summary.summaryUsage.SummaryApplicationCPA;
import org.sosy_lab.cpachecker.cpa.summary.summaryUsage.SummaryComputationRequest;
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
public class TopLevelSummaryCPA extends AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysis,
               AbstractDomain,
               TransferRelation,
               PrecisionAdjustment,
               MergeOperator,
               StatisticsProvider {

  // todo: this flag is ugly
  @Option(secure=true, description="Whether summaries should be merged")
  private boolean joinSummaries = false;

  private final SummaryApplicationCPA wrapped;

  private final CPAAlgorithmFactory algorithmFactory;
  private final ReachedSetFactory reachedSetFactory;
  private final BlockManager blockManager;
  private final SummaryManager wrappedSummaryManager;
  private final SummaryComputationStatistics statistics;
  private final LogManager logger;
  private final CFA cfa;
  private final SummaryWaitlist summaryWaitlist;

  /**
   * Function name to stored summaries mapping.
   *
   * <p>Maintains an invariant that no stored summary strictly
   * subsumes the other one.
   */
  private final SummaryStorage summaryStorage;

  public TopLevelSummaryCPA(
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

    summaryStorage = new SummaryStorage();
    logger = pLogger;
    statistics = new SummaryComputationStatistics(summaryStorage, pConfig);
    blockManager = new BlockManager(pCfa, pConfig, pLogger);
    wrapped = new SummaryApplicationCPA(
        pWrapped, summaryStorage, blockManager, pLogger);
    wrappedSummaryManager = wrapped.getSummaryManager();

    algorithmFactory = new CPAAlgorithmFactory(
        wrapped, pLogger, pConfig, pShutdownNotifier
    );
    summaryWaitlist = new SummaryWaitlist(wrappedSummaryManager, wrapped.getAbstractDomain());
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
      final SummaryComputationState summaryComputationState)
      throws CPAException, InterruptedException {

    // todo: need a way to visualize each individual analysis!!
    ReachedSet reached = summaryComputationState.getReached();

    logger.log(Level.INFO, "Processing computation request for stateId=",
        summaryComputationState.getStateId(),
        "blockName='",
        summaryComputationState.getBlock().getName(), "'");
    CPAAlgorithm algorithm = algorithmFactory.newInstance();

    // todo: check the algorithm status.
    AlgorithmStatus status = algorithm.run(reached);
    assert status.isSound();

    // Requests for summary computation is not empty:
    // put the new requests into the priority queue,
    // as well as the updated request for the currently processed function.
    // Coverage within the computation requests is taken care of using the domain.
    List<SummaryComputationState> toReturn = new ArrayList<>();
    boolean allCallsCovered = wrapped.getSummaryComputationRequests().isEmpty();

    for (SummaryComputationRequest req : Iterables.consumingIterable(
                                  wrapped.getSummaryComputationRequests())) {

      if (!req.isUnsoundSummaryAvailable()) {
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
            summaryComputationState,
            req.getCallEdge(),
            summaryStorage.getTimestamp());

        toReturn.add(scs);
      }

      summaryWaitlist.registerDependency(
          req.getCallingContext(), summaryComputationState
      );
    }
    summaryComputationState.setIsExpanded();

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
      logger.log(Level.INFO, "Has target state = " + hasTargetState,
          " has waiting state = " + hasWaitingState + " returning same state");
      return Collections.singleton(
          summaryComputationState.withUpdatedTargetable(
              hasWaitingState,
              hasTargetState,
              pViolatedProperties,
              summaryStorage.getTimestamp()));

    } else {

      returnStates = AbstractStates.filterLocation(
          reached, summaryComputationState.getBlock().getExitNode()
      ).filter(ARGState.class).filter(s -> s.getChildren().isEmpty()).toList();
    }

    // Generate the summaries if we're not in the outer function,
    // and there was a way to a "return" node.
    Collection<? extends Summary> generatedSummaries;
    if (!returnStates.isEmpty() &&
        !summaryComputationState.getBlock().getName().equals(
            cfa.getMainFunction().getFunctionName())) {

      generatedSummaries = wrappedSummaryManager.generateSummaries(
          summaryComputationState.getCallingContext().get(),
          summaryComputationState.getEntryPrecision(),
          returnStates,
          returnStates.stream().map(s -> reached.getPrecision(s)).collect(Collectors.toList()),
          AbstractStates.extractLocation(summaryComputationState.getCallingContext().get()),
          summaryComputationState.getBlock()
      );
      logger.log(Level.INFO, "Generated summaries: ", generatedSummaries);

    } else {

      // Otherwise, assume "unreachable".
      generatedSummaries = Collections.emptyList();
    }

    Set<Summary> storedSummaries = new HashSet<>();

    for (Summary s : generatedSummaries) {
      storedSummaries.addAll(
          storeGeneratedSummary(summaryComputationState, s, allCallsCovered)
      );
    }

    // Re-enqueue states.
    Collection<SummaryComputationState> toRecompute =
        summaryWaitlist.getToRecompute(
            storedSummaries,
            summaryStorage.getTimestamp(),
            summaryComputationState);
    toReturn.addAll(toRecompute);

    return toReturn;
  }

  /**
   * Store the generated summary in the {@link #summaryStorage} datastructure.
   *
   * <p>Merges the summary with the existing ones,
   * and additionally perform the coverage check.
   * The logic duplicates that of {@link CPAAlgorithm}
   * for the merge and the subsequent coverage check.
   *
   *
   * @param pSummaryComputationState Summary computation state which resulted in this
   *                                 summary generation.
   * @param generatedSummary Summary which was generated.
   * @param isSound Whether the summary is sound: this is true iff
   *                the summary correctly over-approximates all the possible
   *                computations within the block
   *                and does not require further computation requests.
   *
   * @return whether the collection was changed (if not, the summary was subsumed).
   */
  @CanIgnoreReturnValue
  private Set<Summary> storeGeneratedSummary(
      SummaryComputationState pSummaryComputationState,
      Summary generatedSummary,
      boolean isSound)
      throws CPAException, InterruptedException {

    Set<Summary> out = new HashSet<>();

    // Do the merge.
    String partition = wrappedSummaryManager.getSummaryPartition(generatedSummary);

    Collection<Summary> matchingSummaries = summaryStorage.get(partition);

    List<Summary> toRemove = new ArrayList<>();

    List<Summary> toAdd = new ArrayList<>();
    for (Summary existingSummary : matchingSummaries) {
      Summary merged = wrappedSummaryManager.merge(generatedSummary, existingSummary);
      if (merged != existingSummary) {
        toRemove.add(existingSummary);
        toAdd.add(merged);

        // todo: how to visualized merged summaries?
        pSummaryComputationState.setGeneratedSummary(generatedSummary);

        // The join of two sound summaries is also sound.
        if (isSound && summaryStorage.isSound(existingSummary)) {
          summaryStorage.addToSound(merged);
        }
      }
    }

    summaryStorage.removeAll(partition, toRemove);
    summaryStorage.addAll(partition, toAdd);
    out.addAll(toAdd);

    // Do the coverage computation.
    boolean add = true;
    for (Summary existingSummary : matchingSummaries) {

      if (wrappedSummaryManager.isDescribedBy(generatedSummary, existingSummary)) {

        // Summary is subsumed, do nothing.
        add = false;
        break;
      }
    }
    if (add) {
      out.add(generatedSummary);
      summaryStorage.add(partition, generatedSummary, isSound);
      pSummaryComputationState.setGeneratedSummary(generatedSummary);
    }

    return out;
  }

  @Override
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    SummaryComputationState sState1 = (SummaryComputationState) state1;
    SummaryComputationState sState2 = (SummaryComputationState) state2;

    if (sState1.isTarget()) {

      // Do not cover target states.
      return false;
    }

    if ((sState1.getEntryState() == sState2.getEntryState()
        || (joinSummaries && wrapped.getAbstractDomain().isLessOrEqual(
              sState1.getEntryState(), sState2.getEntryState())))
        && sState1.getSummaryStorageTimestamp() <= sState2.getSummaryStorageTimestamp()) {
      sState1.setCoveredBy(sState2);
      return true;
    }

    return false;
  }

  @Override
  public AbstractState merge(
      AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {

    // todo: merge summary computation requests for performance when "joinSummaries" is set.
    return state2;
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
    return AutomaticCPAFactory.forType(TopLevelSummaryCPA.class);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
    wrapped.collectStatistics(statsCollection);
  }

}
