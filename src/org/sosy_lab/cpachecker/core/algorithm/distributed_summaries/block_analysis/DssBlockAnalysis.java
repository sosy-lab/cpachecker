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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedSet;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
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
      boolean isSound,
      Collection<StateAndPrecision> summaries,
      Collection<AbstractState> violationConditions) {}

  private record SoundOrUnsoundPreconditions(
      boolean isSound, boolean containsTopState, List<StateAndPrecision> preconditions) {}

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final DssMessageFactory messageFactory;
  private final Multimap<String, @NonNull StateAndPrecision> preconditions;
  private final Multimap<String, @NonNull StateAndPrecision> violationConditions;
  private final SequencedSet<String> soundPredecessors;

  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Algorithm algorithm;

  private final LogManager logger;

  private AlgorithmStatus status;
  private boolean isOriginal;

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
    soundPredecessors = new LinkedHashSet<>();

    isOriginal = false;
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
            AggregatedReachedSets.empty(),
            cfa);

    ConfigurableProgramAnalysis cpa = coreComponents.createCPA(specification);
    Optional.ofNullable(CPAs.retrieveCPA(cpa, BlockCPA.class)).ifPresent(b -> b.init(node));
    Algorithm algorithm = coreComponents.createAlgorithm(cpa, specification);

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

  /**
   * Serialize a list of states and precisions into a map of strings. Every entry in the list will
   * be serialized under its own key (prefixed by state#num. The {@link #deserialize(DssMessage)}
   * method restores the list of states and precisions.
   *
   * @param pStatesAndPrecisions List of abstract states and their corresponding precision.
   * @return Map of strings representing the serialized states and precisions. Every state will be
   *     serialized with the given serialize operators but all keys will be prefixed with state#num.
   */
  private ImmutableMap<String, String> serialize(
      final List<@NonNull StateAndPrecision> pStatesAndPrecisions) {
    ContentBuilder serializedContent = ContentBuilder.builder();
    serializedContent.put(
        DistributedConfigurableProgramAnalysis.MULTIPLE_STATES_KEY,
        Integer.toString(pStatesAndPrecisions.size()));
    for (int i = 0; i < pStatesAndPrecisions.size(); i++) {
      serializedContent.pushLevel(SerializeOperator.STATE_KEY + i);
      StateAndPrecision stateAndPrecision = pStatesAndPrecisions.get(i);
      ImmutableMap<String, String> content =
          ImmutableMap.<String, String>builder()
              .putAll(dcpa.getSerializeOperator().serialize(stateAndPrecision.state()))
              .putAll(
                  dcpa.getSerializePrecisionOperator()
                      .serializePrecision(stateAndPrecision.precision()))
              .buildOrThrow();
      for (Entry<String, String> contents : content.entrySet()) {
        serializedContent.put(contents.getKey(), contents.getValue());
      }
      serializedContent.popLevel();
    }
    return serializedContent.build();
  }

  /**
   * The method restores a lis of states and precisions from a DssMessage. In general, it should
   * hold that the concretization of the list of states is a subset of the concretization after
   * serializing and deserializing them, i.e., [[states]] <= [[deserialize(serialize(states))]].
   *
   * @param pMessage The message with potentially multiple abstract states to deserialize
   * @return A list of StateAndPrecision objects restored from the message.
   * @throws InterruptedException If the deserialization is interrupted.
   */
  private ImmutableList<@NonNull StateAndPrecision> deserialize(final DssMessage pMessage)
      throws InterruptedException {
    OptionalInt optionalNumberOfStates = pMessage.getNumberOfContainedStates();
    if (optionalNumberOfStates.isEmpty()) {
      return ImmutableList.of();
    }
    int numStates = optionalNumberOfStates.orElseThrow();
    ImmutableList.Builder<StateAndPrecision> statesAndPrecisions =
        ImmutableList.builderWithExpectedSize(numStates);
    for (int i = 0; i < numStates; i++) {
      DssMessage advancedMessage = pMessage.advance(DeserializeOperator.STATE_KEY + i);
      AbstractState state = dcpa.getDeserializeOperator().deserialize(advancedMessage);
      Precision precision =
          dcpa.getDeserializePrecisionOperator().deserializePrecision(advancedMessage);
      statesAndPrecisions.add(new StateAndPrecision(state, precision));
    }
    return statesAndPrecisions.build();
  }

  private Collection<ARGPath> collectPaths(Iterable<@NonNull ARGState> states) {
    if (forcefullyCollectAllArgPaths) {
      return ARGUtils.collectAllArgPaths(states);
    }
    ImmutableList.Builder<ARGPath> paths = ImmutableList.builder();
    for (ARGState state : states) {
      paths.addAll(ARGUtils.getAllPaths(reachedSet, state));
    }
    return paths.build();
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
        messageFactory.createDssPostConditionMessage(
            block.getId(),
            false,
            true,
            status,
            ImmutableList.copyOf(block.getSuccessorIds()),
            ImmutableMap.of()));
  }

  private ImmutableList<@NonNull StateAndPrecision> deduplicateStates(
      Collection<@NonNull StateAndPrecision> summaries) throws InterruptedException, CPAException {
    // reset all summaries and run cpa algorithm on them to remove redundant ones
    if (summaries.size() < 2) {
      return ImmutableList.copyOf(summaries);
    }
    List<StateAndPrecision> sps = new ArrayList<>(summaries.size());
    ImmutableMap.Builder<StateAndPrecision, AbstractState> resetStatesBuilder =
        ImmutableMap.builderWithExpectedSize(summaries.size());
    for (StateAndPrecision summary : summaries) {
      AbstractState reset = dcpa.reset(summary.state());
      resetStatesBuilder.put(summary, reset);
      sps.add(new StateAndPrecision(reset, makeStartPrecision()));
    }
    ImmutableMap<StateAndPrecision, AbstractState> resetStates = resetStatesBuilder.buildOrThrow();
    reachedSet.clear();
    DssBlockAnalyses.executeCpaAlgorithmWithStates(reachedSet, cpa, sps);
    ImmutableSet<AbstractState> reachedSetStates = ImmutableSet.copyOf(reachedSet.asCollection());
    reachedSet.clear();

    // filter the kept states
    ImmutableList.Builder<StateAndPrecision> finalStates = ImmutableList.builder();
    for (StateAndPrecision summary : summaries) {
      if (reachedSetStates.contains(resetStates.get(summary))) {
        finalStates.add(summary);
      }
    }
    ImmutableList<StateAndPrecision> uniqueSummaries = finalStates.build();
    if (uniqueSummaries.isEmpty()) {
      throw new AssertionError("No unique summaries found after CPA run");
    }
    return uniqueSummaries;
  }

  private Collection<DssMessage> reportPostconditions(
      Collection<@NonNull StateAndPrecision> summaries, boolean isSound)
      throws CPAException, InterruptedException {
    isSound &= soundPredecessors.containsAll(block.getPredecessorIds());

    // reset all summaries and run cpa algorithm on them to remove redundant ones
    ImmutableList<StateAndPrecision> uniqueSummaries = deduplicateStates(summaries);

    if (uniqueSummaries.isEmpty()) {
      throw new AssertionError("No unique summaries found after CPA run");
    }

    // pack the message
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    ImmutableMap<String, String> serialized = serialize(uniqueSummaries);
    messages.add(
        messageFactory.createDssPostConditionMessage(
            block.getId(),
            true,
            isSound,
            status,
            ImmutableList.copyOf(block.getSuccessorIds()),
            serialized));
    return messages.build();
  }

  private Collection<DssMessage> reportFirstViolationConditions(Set<@NonNull ARGState> violations)
      throws CPAException, InterruptedException, SolverException {
    isOriginal = true;
    return reportViolationConditions(computeViolationConditionStatesFromOrigin(violations), true);
  }

  private Collection<DssMessage> reportViolationConditions(
      Collection<AbstractState> relevantViolations, boolean first) throws InterruptedException {
    ImmutableList.Builder<StateAndPrecision> vcs = ImmutableList.builder();
    for (AbstractState relevantViolation : relevantViolations) {
      vcs.add(new StateAndPrecision(relevantViolation, makeStartPrecision()));
    }
    ImmutableMap<String, String> serialized = serialize(vcs.build());
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

    if (result.getAllViolations().isEmpty()) {
      if (result.getFinalLocationStates().isEmpty()) {
        return reportUnreachableBlockEnd();
      }
      ImmutableList.Builder<StateAndPrecision> summariesWithPrecision = ImmutableList.builder();
      for (AbstractState finalState : result.getFinalLocationStates()) {
        summariesWithPrecision.add(
            new StateAndPrecision(finalState, reachedSet.getPrecision(finalState)));
      }
      return reportPostconditions(summariesWithPrecision.build(), true);
    }

    ImmutableList.Builder<DssMessage> messages = ImmutableList.builder();
    if (result.getFinalLocationStates().isEmpty()) {
      messages.addAll(reportUnreachableBlockEnd());
    }
    return messages.addAll(reportFirstViolationConditions(result.getAllViolations())).build();
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
  public DssMessageProcessing storePrecondition(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    logger.log(Level.INFO, "Running forward analysis with new precondition");
    if (!pReceived.isReachable()) {
      soundPredecessors.add(pReceived.getSenderId());
      preconditions.removeAll(pReceived.getSenderId());
      if (block.getPredecessorIds().size() == 1) {
        return DssMessageProcessing.stopWith(reportUnreachableBlockEnd());
      }
      return DssMessageProcessing.stop();
    }
    List<StateAndPrecision> deserializedStates = deserialize(pReceived);
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

    resetStates();
    ImmutableSet.Builder<PredecessorStateEntry> discard = ImmutableSet.builder();
    int covered = 0;
    for (StateAndPrecision deserialized : deserializedStates) {
      if (dcpa.isMostGeneralBlockEntryState(deserialized.state())) {
        soundPredecessors.add(pReceived.getSenderId());
        preconditions.removeAll(pReceived.getSenderId());
        preconditions.putAll(pReceived.getSenderId(), deserializedStates);
        return DssMessageProcessing.proceed();
      }
      boolean isEquivalent = false;
      for (StateAndPrecision previous : preconditions.get(pReceived.getSenderId())) {
        if (dcpa.isMostGeneralBlockEntryState(previous.state())) {
          discard.add(new PredecessorStateEntry(pReceived.getSenderId(), previous));
        }
        // the reset resets the callstack state, too
        boolean previousLessEqualDeserialized =
            dcpa.getCoverageOperator()
                .isSubsumed(previous.state(), dcpa.reset(deserialized.state()));
        if (previousLessEqualDeserialized) {
          boolean deserializedLessEqualPrevious =
              dcpa.getCoverageOperator()
                  .isSubsumed(dcpa.reset(deserialized.state()), previous.state());
          if (deserializedLessEqualPrevious) {
            if (pReceived.isSound()) {
              soundPredecessors.add(pReceived.getSenderId());
            }
            isEquivalent = true;
          }
          discard.add(new PredecessorStateEntry(pReceived.getSenderId(), previous));
        }
      }
      if (isEquivalent) {
        covered++;
      }
    }
    // summaries from non-loop predecessors are by definition stronger and unique
    if (!block.getLoopPredecessorIds().contains(pReceived.getSenderId())) {
      for (StateAndPrecision sp : preconditions.get(pReceived.getSenderId())) {
        discard.add(new PredecessorStateEntry(pReceived.getSenderId(), sp));
      }
    }

    ImmutableSet<PredecessorStateEntry> discarded = discard.build();
    preconditions.putAll(pReceived.getSenderId(), deserializedStates);
    discarded.forEach(pse -> preconditions.remove(pse.predecessorId(), pse.stateAndPrecision()));

    int coveredByOther = 0;
    for (StateAndPrecision deserializedState : deserializedStates) {
      for (String predecessor : preconditions.keySet()) {
        if (predecessor.equals(pReceived.getSenderId())) {
          continue;
        }
        boolean isCovered = false;
        for (StateAndPrecision stateAndPrecision : preconditions.get(predecessor)) {
          isCovered =
              dcpa.getCoverageOperator()
                  .isSubsumed(stateAndPrecision.state(), dcpa.reset(deserializedState.state()));
          if (isCovered) {
            break;
          }
        }
        if (isCovered) {
          coveredByOther++;
          break;
        }
      }
    }
    if (coveredByOther == deserializedStates.size()) {
      // we reached a fixpoint where all predecessors agree.
      soundPredecessors.add(pReceived.getSenderId());
      return DssMessageProcessing.stop();
    }

    if (covered == deserializedStates.size()) {
      // we already have a precondition equivalent to the new one
      if (soundPredecessors.containsAll(block.getPredecessorIds())) {
        if (pReceived.isSound()) {
          soundPredecessors.add(pReceived.getSenderId());
        } else {
          soundPredecessors.remove(pReceived.getSenderId());
        }
        return DssMessageProcessing.stop();
      } else {
        soundPredecessors.add(block.getId());
      }
    }
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
    ImmutableList<StateAndPrecision> deserializedStates = deserialize(pNewViolationCondition);
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
    ImmutableList.Builder<StateAndPrecision> soundSummaries = ImmutableList.builder();
    ImmutableList.Builder<StateAndPrecision> unsoundSummaries = ImmutableList.builder();
    if (isOriginal || !violationConditions.isEmpty()) {
      AnalysisResult result =
          analyzeViolationCondition(
              transformedImmutableListCopy(
                  isOriginal ? ImmutableSet.of() : violationConditions.values(),
                  v -> (ARGState) v.state()));
      if (!result.violationConditions().isEmpty()) {
        messages.addAll(reportViolationConditions(result.violationConditions(), false));
      } else {
        if (result.isSound()) {
          soundSummaries.addAll(result.summaries());
        } else {
          unsoundSummaries.addAll(result.summaries());
        }
      }
    }
    ImmutableList<StateAndPrecision> states = soundSummaries.build();
    if (!states.isEmpty()) {
      messages.addAll(reportPostconditions(states, true));
    }
    ImmutableList<StateAndPrecision> unsoundStates = unsoundSummaries.build();
    if (!unsoundStates.isEmpty()) {
      messages.addAll(reportPostconditions(unsoundStates, false));
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
      messages.addAll(
          reportPostconditions(
              result.summaries(), result.isSound() && result.violationConditions().isEmpty()));
    }
    if (!result.violationConditions().isEmpty()) {
      messages.addAll(reportViolationConditions(result.violationConditions(), false));
    }
    if (result.summaries().isEmpty() && result.violationConditions().isEmpty()) {
      messages.addAll(reportUnreachableBlockEnd());
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
    if (preconditions.isEmpty() && !block.isRoot()) {
      return new AnalysisResult(true, ImmutableList.of(), ImmutableList.of());
    }
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableList.Builder<AbstractState> vcs = ImmutableList.builder();
    SoundOrUnsoundPreconditions prepped = prepareReachedSet();
    int i = 0;
    for (StateAndPrecision stateAndPrecision : prepped.preconditions()) {
      i++;
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
        if (!prepped.containsTopState() || i == 1) {
          if (!result.getAllViolations().isEmpty()) {
            vcs.addAll(computeViolationConditionStates(result.getViolationConditionViolations()));
            if (isOriginal) {
              vcs.addAll(computeViolationConditionStatesFromOrigin(result.getTargetStates()));
            }
          }
        }
      } else {
        vcs.addAll(
            computeViolationConditionStatesFromBlockEnd(
                result.getFinalLocationStates(), violations));
      }
    }
    return new AnalysisResult(prepped.isSound(), summaries.build(), vcs.build());
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
  private SoundOrUnsoundPreconditions prepareReachedSet()
      throws CPAException, InterruptedException {
    // clear stateful data structures
    reachedSet.clear();
    resetStates();
    boolean isSound = true;

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
            isSound = false;
            break;
          }
        }
      }

      // if not, add the states to the precondition
      if (putStates) {
        for (StateAndPrecision stateAndPrecision : statesAndPrecisions) {
          if (dcpa.isMostGeneralBlockEntryState(stateAndPrecision.state())) {
            isSound = true;
          }
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
      return new SoundOrUnsoundPreconditions(
          true,
          true,
          ImmutableList.of(new StateAndPrecision(makeStartState(), makeStartPrecision())));
    }
    ImmutableList.Builder<StateAndPrecision> toProcess = ImmutableList.builder();
    boolean containsTopState =
        (!isSound || block.allPredecessorsAreLoopPredecessors())
            && !soundPredecessors.containsAll(block.getPredecessorIds());
    if (containsTopState) {
      toProcess.add(new StateAndPrecision(makeStartState(), makeStartPrecision()));
    }
    reachedSet.forEach((s, p) -> toProcess.add(new StateAndPrecision(s, p)));
    reachedSet.clear();
    return new SoundOrUnsoundPreconditions(isSound, containsTopState, toProcess.build());
  }

  public DistributedConfigurableProgramAnalysis getDcpa() {
    return dcpa;
  }
}
