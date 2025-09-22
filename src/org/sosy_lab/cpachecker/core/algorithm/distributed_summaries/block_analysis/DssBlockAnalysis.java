// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.collectAllArgPaths;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPreconditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.java_smt.api.SolverException;

public class DssBlockAnalysis {

  private record AnalysisComponents(
      Algorithm algorithm, ConfigurableProgramAnalysis cpa, ReachedSet reached) {}

  private record AnalysisResult(
      Collection<StateAndPrecision> summaries, Collection<AbstractState> violationConditions) {}

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final DssMessageFactory messageFactory;
  private final Multimap<String, @NonNull StateAndPrecision> preconditions;
  private final Multimap<String, @NonNull StateAndPrecision> violationConditions;

  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Algorithm algorithm;

  private final LogManager logger;

  private AlgorithmStatus status;

  private final boolean forcefullyCollectAllArgPaths;

  public DssBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    messageFactory = pMessageFactory;
    AnalysisComponents parts =
        createBlockAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    // prepare dcpa and the algorithms
    status = AlgorithmStatus.SOUND_AND_PRECISE;
    algorithm = parts.algorithm();
    cpa = parts.cpa();
    block = pBlock;
    logger = pLogger;
    dcpa =
        DssFactory.distribute(
            cpa,
            pBlock,
            pCFA,
            pConfiguration,
            pOptions,
            pMessageFactory,
            pLogger,
            pShutdownManager.getNotifier());
    // prepare reached set and initial elements
    reachedSet = parts.reached();
    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    reachedSet.clear();
    assert dcpa != null : "Distribution of " + cpa.getClass().getSimpleName() + " not implemented.";

    preconditions = ArrayListMultimap.create();
    violationConditions = ArrayListMultimap.create();
    forcefullyCollectAllArgPaths = pOptions.forcefullyCollectAllViolationConditions();
  }

  /**
   * Creates the CPA algorithm to be used for the analysis of the given block node.
   *
   * @param logger the logger to use
   * @param specification the specification to use
   * @param cfa the CFA to use
   * @param globalConfig the global configuration to use for DSS
   * @param singleShutdownManager the shutdown manager to use
   * @param node the block node to analyze
   * @return the analysis components to use for the analysis of the block node
   * @throws InvalidConfigurationException if the configuration is invalid
   * @throws CPAException if the CPA cannot be created
   * @throws InterruptedException if the thread is interrupted
   */
  private static AnalysisComponents createBlockAlgorithm(
      final LogManager logger,
      final Specification specification,
      final CFA cfa,
      final Configuration globalConfig,
      final ShutdownManager singleShutdownManager,
      final BlockNode node)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    LogManager singleLogger = logger.withComponentName("Analysis " + node);

    ResourceLimitChecker singleLimits =
        ResourceLimitChecker.fromConfiguration(globalConfig, singleLogger, singleShutdownManager);
    singleLimits.start();

    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            globalConfig,
            singleLogger,
            singleShutdownManager.getNotifier(),
            AggregatedReachedSets.empty());

    ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, specification);
    Optional.ofNullable(CPAs.retrieveCPA(cpa, BlockCPA.class)).ifPresent(b -> b.init(node));
    Algorithm algorithm = coreComponents.createAlgorithm(cpa, cfa, specification);

    singleLogger.log(Level.FINE, "Creating initial reached set");
    AbstractState initialState =
        cpa.getInitialState(node.getInitialLocation(), StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(
            node.getInitialLocation(), StateSpacePartition.getDefaultPartition());
    ReachedSet reached = coreComponents.createReachedSet(cpa);
    reached.add(initialState, initialPrecision);

    return new AnalysisComponents(algorithm, cpa, reached);
  }

  private Collection<ARGPath> collectPaths(Iterable<@NonNull ARGState> states) {
    if (forcefullyCollectAllArgPaths) {
      return collectAllArgPaths(states);
    }
    ImmutableList.Builder<ARGPath> builder = ImmutableList.builder();
    for (ARGState state : states) {
      builder.addAll(ARGUtils.getAllPaths(reachedSet, state));
    }
    return builder.build();
  }

  private List<AbstractState> computeViolationConditionStatesFromOrigin(
      Collection<@NonNull ARGState> state)
      throws CPATransferException, SolverException, InterruptedException {
    ImmutableList.Builder<AbstractState> relevantViolations = ImmutableList.builder();
    for (ARGPath path : collectPaths(state)) {
      Optional<AbstractState> violationCondition =
          dcpa.getViolationConditionOperator().computeViolationCondition(path, Optional.empty());
      violationCondition.ifPresent(relevantViolations::add);
    }
    return relevantViolations.build();
  }

  private List<AbstractState> computeViolationConditionStatesFromBlockEnd(
      Collection<@NonNull ARGState> violations, Collection<@NonNull ARGState> conditions)
      throws CPATransferException, SolverException, InterruptedException {
    ImmutableList.Builder<AbstractState> relevantViolations = ImmutableList.builder();
    for (ARGState violation : violations) {
      for (ARGPath path : collectPaths(ImmutableList.of(violation))) {
        for (ARGState condition : conditions) {
          Optional<AbstractState> violationCondition =
              dcpa.getViolationConditionOperator()
                  .computeViolationCondition(path, Optional.of(condition));
          violationCondition.ifPresent(relevantViolations::add);
        }
      }
    }
    return relevantViolations.build();
  }

  private List<AbstractState> computeViolationConditionStates(
      Collection<@NonNull ARGState> violations)
      throws CPATransferException, SolverException, InterruptedException {
    ImmutableList.Builder<AbstractState> relevantViolations = ImmutableList.builder();
    for (ARGState violation : violations) {
      BlockState condition =
          Objects.requireNonNull(AbstractStates.extractStateByType(violation, BlockState.class));
      ARGState violationState =
          (ARGState) Iterables.getOnlyElement(condition.getViolationConditions());
      for (ARGPath path : collectPaths(ImmutableList.of(violation))) {
        Optional<AbstractState> violationCondition =
            dcpa.getViolationConditionOperator()
                .computeViolationCondition(path, Optional.of(violationState));
        violationCondition.ifPresent(relevantViolations::add);
      }
    }
    return relevantViolations.build();
  }

  private Collection<DssMessage> reportUnreachableBlockEnd() {
    return ImmutableSet.of(
        messageFactory.createDssPreconditionMessage(
            block.getId(),
            false,
            status,
            ImmutableList.copyOf(block.getSuccessorIds()),
            ImmutableMap.of()));
  }

  private Collection<DssMessage> reportPreconditions(
      Collection<? extends @NonNull StateAndPrecision> summaries, boolean allowTop) {
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    for (StateAndPrecision abstraction : summaries) {
      if (dcpa.isMostGeneralBlockEntryState(abstraction.state()) && !allowTop) {
        return messages.build();
      }
    }
    ImmutableMap<String, String> serialized = dcpa.serialize(ImmutableList.copyOf(summaries));
    messages.add(
        messageFactory.createDssPreconditionMessage(
            block.getId(),
            true,
            status,
            ImmutableList.copyOf(block.getSuccessorIds()),
            serialized));
    return messages.build();
  }

  private Collection<DssMessage> reportFirstViolationConditions(Set<@NonNull ARGState> violations)
      throws CPAException, InterruptedException, SolverException {
    return reportViolationConditions(computeViolationConditionStatesFromOrigin(violations), true);
  }

  private Collection<DssMessage> reportViolationConditions(
      Collection<AbstractState> relevantViolations, boolean first) throws InterruptedException {
    ImmutableList.Builder<StateAndPrecision> vcs = ImmutableList.builder();
    for (AbstractState relevantViolation : relevantViolations) {
      vcs.add(new StateAndPrecision(relevantViolation, makeStartPrecision()));
    }
    ImmutableMap<String, String> serialized = dcpa.serialize(vcs.build());
    return ImmutableSet.of(
        messageFactory.createViolationConditionMessage(block.getId(), status, first, serialized));
  }

  /**
   * Executes the configured CPA algorithm on the block with the initial state and precision.
   *
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   * @throws SolverException thrown if solver runs into an error
   */
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    reachedSet.clear();
    reachedSet.add(makeStartState(), makeStartPrecision());

    DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    if (result.getViolations().isEmpty()) {
      if (result.getFinalLocationStates().isEmpty()) {
        return reportUnreachableBlockEnd();
      }
      ImmutableList.Builder<StateAndPrecision> summariesWithPrecision = ImmutableList.builder();
      for (AbstractState finalState : result.getFinalLocationStates()) {
        summariesWithPrecision.add(
            new StateAndPrecision(finalState, reachedSet.getPrecision(finalState)));
      }
      return reportPreconditions(summariesWithPrecision.build(), true);
    }

    return reportFirstViolationConditions(result.getViolations());
  }

  /**
   * Adds a new precondition to the known preconditions. The method checks whether the new
   * precondition is already covered by an existing one. If this is the case, the new precondition
   * is discarded and the analysis will not proceed. Otherwise, the new precondition is added and
   * the analysis will proceed.
   *
   * @param pReceived The new precondition to add.
   * @return Whether the analysis should proceed.
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   * @throws SolverException thrown if solver runs into an error
   * @throws CPAException thrown if CPA runs into an error
   */
  public DssMessageProcessing storePrecondition(DssPreconditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    logger.log(Level.INFO, "Running forward analysis with new precondition");
    if (!pReceived.isReachable()) {
      preconditions.removeAll(pReceived.getSenderId());
      return DssMessageProcessing.stop();
    }
    List<StateAndPrecision> deserializedStates = dcpa.deserialize(pReceived);
    DssMessageProcessing processing = DssMessageProcessing.proceed();
    for (StateAndPrecision stateAndPrecision : deserializedStates) {
      processing =
          processing.merge(
              dcpa.getProceedOperator().processForward(stateAndPrecision.state()), true);
    }
    if (!processing.shouldProceed()) {
      return processing;
    }

    record PredecessorStateEntry(String predecessorId, StateAndPrecision stateAndPrecision) {}

    ImmutableSet.Builder<PredecessorStateEntry> discard = ImmutableSet.builder();
    int covered = 0;
    for (StateAndPrecision deserialized : deserializedStates) {
      for (Entry<String, StateAndPrecision> previousEntry : preconditions.entries()) {
        StateAndPrecision previous = previousEntry.getValue();
        if (dcpa.isMostGeneralBlockEntryState(previous.state())) {
          discard.add(new PredecessorStateEntry(previousEntry.getKey(), previousEntry.getValue()));
          continue;
        }
        if (dcpa.getCoverageOperator().isSubsumed(previous.state(), deserialized.state())) {
          covered++;
          discard.add(new PredecessorStateEntry(pReceived.getSenderId(), deserialized));
          break;
        }
        if (dcpa.getCoverageOperator().isSubsumed(deserialized.state(), previous.state())) {
          discard.add(new PredecessorStateEntry(previousEntry.getKey(), previousEntry.getValue()));
        }
      }
    }
    if (covered == deserializedStates.size()) {
      // we already have a precondition implying the new one
      return DssMessageProcessing.stop();
    }
    if (!block.getLoopPredecessorIds().contains(pReceived.getSenderId())) {
      for (StateAndPrecision sp : preconditions.get(pReceived.getSenderId())) {
        discard.add(new PredecessorStateEntry(pReceived.getSenderId(), sp));
      }
    }
    ImmutableSet<PredecessorStateEntry> discarded = discard.build();
    preconditions.putAll(pReceived.getSenderId(), deserializedStates);
    discarded.forEach(pse -> preconditions.remove(pse.predecessorId(), pse.stateAndPrecision()));
    return processing;
  }

  /**
   * Adds a new abstract state to the known violation conditions.
   *
   * @param pNewViolationCondition The new violation condition to add.
   * @return Whether the analysis should proceed.
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   * @throws SolverException thrown if solver runs into an error
   */
  public DssMessageProcessing storeViolationCondition(
      DssViolationConditionMessage pNewViolationCondition)
      throws InterruptedException, SolverException {
    logger.log(Level.INFO, "Running forward analysis with respect to error condition");
    // merge all states into the reached set
    ImmutableList<StateAndPrecision> deserializedStates = dcpa.deserialize(pNewViolationCondition);
    violationConditions.removeAll(pNewViolationCondition.getSenderId());
    for (StateAndPrecision stateAndPrecision : deserializedStates) {
      DssMessageProcessing current =
          dcpa.getProceedOperator().processBackward(stateAndPrecision.state());
      if (current.shouldProceed()) {
        violationConditions.put(pNewViolationCondition.getSenderId(), stateAndPrecision);
      }
    }
    return violationConditions.get(pNewViolationCondition.getSenderId()).isEmpty()
        ? DssMessageProcessing.stop()
        : DssMessageProcessing.proceed();
  }

  /**
   * Adds a new abstract state to the known start states and execute the configured forward
   * analysis.
   *
   * @return All violations and/or abstractions that occurred while running the forward analysis.
   */
  public Collection<DssMessage> analyzePrecondition()
      throws SolverException, InterruptedException, CPAException {
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    for (String successor : violationConditions.keys()) {
      AnalysisResult result =
          analyzeViolationCondition(
              transformedImmutableListCopy(
                  violationConditions.get(successor), v -> (ARGState) v.state()));
      if (!result.violationConditions().isEmpty()) {
        messages.addAll(reportViolationConditions(result.violationConditions(), false));
      } else {
        summaries.addAll(result.summaries());
      }
    }
    ImmutableList<StateAndPrecision> states = summaries.build();
    if (!states.isEmpty()) {
      messages.addAll(reportPreconditions(states, false));
    }
    return messages.build();
  }

  /**
   * Analyzes the violation condition for the given sender ID. The violation condition is extracted
   * from the violation conditions stored via {@link
   * #storeViolationCondition(DssViolationConditionMessage)}
   *
   * @param pSenderId Sender ID of the violation-condition message to analyze.
   * @return The messages resulting from the analysis of the violation condition.
   */
  public Collection<DssMessage> analyzeViolationCondition(String pSenderId)
      throws SolverException, InterruptedException, CPAException {
    Collection<@NonNull StateAndPrecision> violations = violationConditions.get(pSenderId);
    if (violations.isEmpty()) {
      throw new IllegalArgumentException(
          "No violation condition found for sender ID: " + pSenderId);
    }
    ImmutableList.Builder<DssMessage> messages = ImmutableList.builder();
    AnalysisResult result =
        analyzeViolationCondition(
            transformedImmutableListCopy(violations, v -> (ARGState) v.state()));
    if (!result.summaries().isEmpty()) {
      messages.addAll(reportPreconditions(result.summaries(), false));
    }
    if (!result.violationConditions().isEmpty()) {
      messages.addAll(reportViolationConditions(result.violationConditions(), false));
    }
    return messages.build();
  }

  /**
   * Runs the CPA under an error condition, i.e., if the current block contains a block-end edge,
   * the error condition will be attached to that edge. In case this makes the path formula
   * infeasible, we compute an abstraction. If no error condition is present, we run the CPA.
   *
   * @param violations The violation condition to analyze, which is a precise summary of all
   *     specification violations
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   */
  private AnalysisResult analyzeViolationCondition(List<ARGState> violations)
      throws CPAException, InterruptedException, SolverException {
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableList.Builder<AbstractState> vcs = ImmutableList.builder();
    for (StateAndPrecision stateAndPrecision : prepareReachedSet()) {
      reachedSet.clear();
      reachedSet.add(stateAndPrecision.state(), stateAndPrecision.precision());
      reachedSet.forEach(
          abstractState ->
              Objects.requireNonNull(
                      AbstractStates.extractStateByType(abstractState, BlockState.class))
                  .setViolationConditions(violations));

      DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

      status = status.update(result.getStatus());

      if (block.isAbstractionPossible()) {
        if (!result.getSummaries().isEmpty()) {
          ImmutableList.Builder<StateAndPrecision> summaryWithPrecision = ImmutableList.builder();
          for (AbstractState summary : result.getSummaries()) {
            summaryWithPrecision.add(
                new StateAndPrecision(summary, reachedSet.getPrecision(summary)));
          }
          summaries.addAll(summaryWithPrecision.build());
        }
      } else {
        vcs.addAll(
            computeViolationConditionStatesFromBlockEnd(
                result.getFinalLocationStates(), violations));
      }
      if (!result.getViolations().isEmpty()) {
        vcs.addAll(computeViolationConditionStates(result.getViolations()));
      }
    }
    return new AnalysisResult(summaries.build(), vcs.build());
  }

  private AbstractState makeStartState() throws InterruptedException {
    return dcpa.getInitialState(
        block.getInitialLocation(), StateSpacePartition.getDefaultPartition());
  }

  private Precision makeStartPrecision() throws InterruptedException {
    return dcpa.getInitialPrecision(
        block.getInitialLocation(), StateSpacePartition.getDefaultPartition());
  }

  /**
   * Resets all preconditions to their initial state, i.e., the ARGState is wrapped in a new
   * ARGState without any parent.
   */
  private void resetStates() {
    for (Entry<String, StateAndPrecision> entry : ImmutableList.copyOf(preconditions.entries())) {
      preconditions.remove(entry.getKey(), entry.getValue());
      preconditions.put(
          entry.getKey(),
          new StateAndPrecision(
              dcpa.reset(entry.getValue().state()), entry.getValue().precision()));
    }
  }

  /**
   * Combines all preconditions into single precision if all precisions are from type
   * AdjustablePrecision.
   *
   * @return combined precision if all preconditions are of type {@link AdjustablePrecision}, empty
   *     otherwise
   */
  private Optional<Precision> combinePrecisionIfPossible() throws InterruptedException {
    if (preconditions.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        dcpa.getCombinePrecisionOperator()
            .combine(
                transformedImmutableListCopy(
                    preconditions.values(), StateAndPrecision::precision)));
  }

  /**
   * Prepare the reached set for next analysis by merging all received preconditions into a
   * non-empty set of start states.
   *
   * @throws CPAException thrown in merge or stop operation runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly.
   */
  private ImmutableList<StateAndPrecision> prepareReachedSet()
      throws CPAException, InterruptedException {
    // clear stateful data structures
    reachedSet.clear();
    resetStates();

    // prepare states to be added to the reached set
    Optional<Precision> combinedPrecision = combinePrecisionIfPossible();
    ImmutableList.Builder<StateAndPrecision> precondition = ImmutableList.builder();
    for (String predecessorId : preconditions.keySet()) {
      Collection<StateAndPrecision> statesAndPrecisions = preconditions.get(predecessorId);
      boolean putStates = true;

      // check whether a loop predecessor is top
      if (block.hasLoopPredecessor(predecessorId) && !block.allPredecessorsAreLoopPredecessors()) {
        for (StateAndPrecision stateAndPrecision : statesAndPrecisions) {
          if (dcpa.isMostGeneralBlockEntryState(stateAndPrecision.state())) {
            putStates = false;
            break;
          }
        }
      }

      // if not, add the states to the precondition
      if (putStates) {
        for (StateAndPrecision stateAndPrecision : statesAndPrecisions) {
          precondition.add(
              new StateAndPrecision(
                  stateAndPrecision.state(),
                  combinedPrecision.orElse(stateAndPrecision.precision())));
        }
      }
    }

    // execute the CPA algorithm with the prepared states at the start location of the block
    DssBlockAnalyses.executeCpaAlgorithmWithStates(reachedSet, cpa, precondition.build());
    if (reachedSet.isEmpty()) {
      reachedSet.add(makeStartState(), makeStartPrecision());
    }
    ImmutableList.Builder<StateAndPrecision> toProcess = ImmutableList.builder();
    reachedSet.forEach((s, p) -> toProcess.add(new StateAndPrecision(s, p)));
    reachedSet.clear();
    return toProcess.build();
  }

  public DistributedConfigurableProgramAnalysis getDcpa() {
    return dcpa;
  }
}
