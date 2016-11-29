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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.UseSummaryCPA;
import org.sosy_lab.cpachecker.cpa.summary.summaryUsage.SummaryComputationRequestState;
import org.sosy_lab.cpachecker.cpa.summary.summaryUsage.TopLevelSummaryCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Top-level CPA for summary generation.
 *
 * Operates over {@link SummaryComputationState} states,
 * stores generated summaries in a stateful multimap.
 *
 */
@Options(prefix="cpa.summary")
public class GenSummaryCPA implements ConfigurableProgramAnalysis, AbstractDomain,
                                      TransferRelation, PrecisionAdjustment {

  @Option(secure=true, description="Whether to join generated summaries")
  private boolean joinSummaries = true;

  private final TopLevelSummaryCPA wrapped;

  // todo: what parameters is this guy created with?
  private final CPAAlgorithmFactory algorithmFactory;

  // todo: what parameters is this guy created with?
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

  /**
   * Function name to stored summaries mapping.
   *
   * <p>Maintains an invariant that no stored summary strictly
   * subsumes the other one.
   */
  private final Multimap<String, Summary> computedSummaries;

  public GenSummaryCPA(
      LogManager logger,
      Configuration config,
      ShutdownNotifier shutdownNotifier,
      ReachedSetFactory pReachedSetFactory,
      CFA pCfa,
      ConfigurableProgramAnalysis pWrapped) throws InvalidConfigurationException {
    config.inject(this);
    reachedSetFactory = pReachedSetFactory;
    cfa = pCfa;
    Preconditions.checkArgument(pWrapped instanceof UseSummaryCPA,
        "Parameter CPA has to implement the SummaryCPA interface.");

    computedSummaries = HashMultimap.create();
    wrapped = new TopLevelSummaryCPA(pWrapped, computedSummaries);
    algorithmFactory = new CPAAlgorithmFactory(
        this, logger, config, shutdownNotifier
    );
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition)
        throws InterruptedException {

    return new SummaryComputationState(
        wrapped.getInitialState(node, partition),
        wrapped.getInitialPrecision(node, partition),
        node
    );
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

  public Collection<SummaryComputationState> getAbstractSuccessors0(
      SummaryComputationState recomputeRequest) throws CPAException, InterruptedException {

    String functionName = recomputeRequest.getFunctionName();

    ReachedSet reached = reachedSetFactory.create();
    reached.add(recomputeRequest.getState(), recomputeRequest.getPrecision());
    CPAAlgorithm algorithm = algorithmFactory.newInstance();

    // todo: check the algorithm status.
    // todo: also check for hasWaitingStates: might be early termination
    // due to the counterexample being found.
    AlgorithmStatus status = algorithm.run(reached);

    FluentIterable<SummaryComputationRequestState> computationRequests =
        AbstractStates.projectToType(reached, SummaryComputationRequestState.class);

    if (computationRequests.isEmpty()) {
      // No new summaries were needed, can finally generate the summary for this function.

      SummaryManager sManager = wrapped.getSummaryManager();

      // Merge the summary with the existing ones,
      // and additionally perform the coverage check.
      // The logic duplicates that of CPAAlgorithm
      // for the merge and the subsequent coverage check.
      Summary generatedSummary = sManager.generateSummary(reached);

      // Do the merge.
      Collection<Summary> matchingSummaries = computedSummaries.get(functionName);
      List<Summary> toRemove = new ArrayList<>();
      List<Summary> toAdd = new ArrayList<>();
      for (Summary existingSummary : matchingSummaries) {
        Summary merged = sManager.merge(generatedSummary, existingSummary);
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
        if (isDescribedBy(generatedSummary, existingSummary)) {

          // Summary is subsumed, do nothing.
          add = false;
          break;
        }
      }
      if (add) {
        matchingSummaries.add(generatedSummary);
      }

      // Summary was completely generated, no successors.
      return ImmutableSet.of();

    } else {
      return computationRequests.transform(
          s -> new SummaryComputationState(
              s.getFunctionEntryState(), s.getFunctionEntryPrecision(), s.getNode())
      ).toList();
    }
  }

  /**
   * Coverage relation for summaries: precondition may be weakened,
   * postcondition may be strengthened.
   * There is no point in storing a summary with a stronger precondition
   * and a weaker postcondition of an already existing one.
   *
   * @return whether {@code pSummary1} is described by {@code pSummary2}.
   */
  private boolean isDescribedBy(Summary pSummary1, Summary pSummary2)
      throws CPAException, InterruptedException {
    AbstractDomain wrappedDomain = wrapped.getAbstractDomain();
    SummaryManager sManager = wrapped.getSummaryManager();

    return wrappedDomain.isLessOrEqual(
        sManager.projectToPrecondition(pSummary1),
        sManager.projectToPrecondition(pSummary2)
    ) && wrappedDomain.isLessOrEqual(
        sManager.projectToPostcondition(pSummary2),
        sManager.projectToPostcondition(pSummary1)
    );
  }

  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {

    // todo: should we perform the join of two requests for generating the summary?
    return null;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {

    // todo: check for target states being found.
    return null;
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
    return null;
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
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {

    // todo: do not store redundant requests for summary recomputation.
    return false;
  }


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("Unexpected API usage");
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(GenSummaryCPA.class);
  }
}
