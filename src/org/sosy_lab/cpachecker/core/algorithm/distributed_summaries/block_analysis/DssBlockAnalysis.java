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
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssBlockWorkerStatistics;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssThreadCpuTimer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
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
import org.sosy_lab.cpachecker.cpa.block.ViolationWitness;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.java_smt.api.SolverException;

public class DssBlockAnalysis {

  private static final class ArgPathAndCondition {

    private final ARGPath path;
    private final @Nullable ARGState condition;

    // Precomputed once because ARGPath/ARGState are immutable and computing the id iterates the
    // full path; caching avoids recomputation on every hashCode/equals call.
    private final String id;

    private ArgPathAndCondition(ARGPath pPath, @Nullable ARGState pCondition) {
      path = pPath;
      condition = pCondition;
      id =
          FluentIterable.from(pPath.getFullPath())
              .transform(edge -> edge.getPredecessor() + "->" + edge.getSuccessor())
              .join(Joiner.on(", "));
    }

    private ARGPath path() {
      return path;
    }

    private @Nullable ARGState condition() {
      return condition;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, condition == null ? null : Objects.toIdentityString(condition));
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      return obj instanceof ArgPathAndCondition other
          && Objects.equals(id, other.id)
          && Objects.equals(
              condition == null ? null : Objects.toIdentityString(condition),
              other.condition() == null ? null : Objects.toIdentityString(other.condition()));
    }
  }

  private record AnalysisComponents(
      Algorithm algorithm, ConfigurableProgramAnalysis cpa, ReachedSet reached) {}

  private record AnalysisResult(
      Collection<StateAndPrecision> summaries, Set<ArgPathAndCondition> violationConditions) {}

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final DssMessageFactory messageFactory;
  private final Multimap<String, @NonNull StateAndPrecision> preconditions;
  private final Multimap<String, @NonNull StateAndPrecision> violationConditions;
  private final List<StateAndPrecision> relevant;

  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Algorithm algorithm;

  private final LogManager logger;

  private final DssThreadCpuTimer storePreconditionTime;
  private final DssThreadCpuTimer analyzePreconditionTime;
  private final DssThreadCpuTimer storeViolationConditionTime;
  private final DssThreadCpuTimer analyzeViolationConditionTime;

  private final StatCounter storePreconditionCount;
  private final StatCounter analyzePreconditionCount;
  private final StatCounter storeViolationConditionCount;
  private final StatCounter analyzeViolationConditionCount;

  private AlgorithmStatus status;
  private boolean containsViolationInsideBlock;

  private final boolean resetPrecisionsForEveryRun;
  private final boolean combineByHash;

  public DssBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      ShutdownManager pShutdownManager,
      DssBlockWorkerStatistics pWorkerStats)
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
    resetPrecisionsForEveryRun = pOptions.resetPrecisionsForEveryRun();
    relevant = new ArrayList<>();

    containsViolationInsideBlock = false;
    combineByHash = pOptions.combineByHash();

    storePreconditionTime = pWorkerStats.getStorePreconditionTimer();
    analyzePreconditionTime = pWorkerStats.getAnalyzePreconditionTimer();
    storeViolationConditionTime = pWorkerStats.getStoreViolationConditionTimer();
    analyzeViolationConditionTime = pWorkerStats.getAnalyzeViolationConditionTimer();
    storePreconditionCount = pWorkerStats.getStorePreconditionCounter();
    analyzePreconditionCount = pWorkerStats.getAnalyzePreconditionCounter();
    storeViolationConditionCount = pWorkerStats.getStoreViolationConditionCounter();
    analyzeViolationConditionCount = pWorkerStats.getAnalyzeViolationConditionCounter();
    // Register dcpa-level statistics with the worker stats object.
    if (dcpa instanceof DistributedARGCPA arg
        && arg.getWrappedCPA() instanceof DistributedCompositeCPA composite) {
      pWorkerStats.setDcpaStatistics(composite.getStatistics());
    }
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

  private Collection<DssMessage> reportPostconditions(
      Collection<@NonNull StateAndPrecision> summaries) throws CPAException, InterruptedException {

    // reset all summaries and run cpa algorithm on them to remove redundant ones
    ImmutableList<StateAndPrecision> uniqueSummaries = deduplicateStates(summaries);

    if (uniqueSummaries.isEmpty()) {
      throw new AssertionError("No unique summaries found after CPA run");
    }

    // pack the message
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    ImmutableMap<String, String> serialized = serialize(uniqueSummaries);
    messages.add(messageFactory.createDssPostConditionMessage(block.getId(), status, serialized));
    return messages.build();
  }

  private Collection<DssMessage> reportFirstViolationConditions(Set<@NonNull ARGState> violations)
      throws CPAException, InterruptedException, SolverException {
    containsViolationInsideBlock = true;
    return reportViolationConditions(computeViolationConditionStatesFromOrigin(violations));
  }

  private Collection<DssMessage> reportViolationConditions(
      Collection<ArgPathAndCondition> relevantViolations)
      throws InterruptedException, CPAException, SolverException {
    record HashAndOrigin(int hash, AbstractState origin) {}
    ImmutableListMultimap.Builder<HashAndOrigin, AbstractState> statePerProgramCounterBuilder =
        ImmutableListMultimap.builder();
    for (ArgPathAndCondition pathAndCondition : relevantViolations) {
      Optional<AbstractState> violationCondition =
          dcpa.getViolationConditionOperator()
              .computeViolationCondition(
                  pathAndCondition.path(), Optional.ofNullable(pathAndCondition.condition()));
      if (violationCondition.isPresent()) {
        statePerProgramCounterBuilder.put(
            new HashAndOrigin(
                dcpa.computeProgramPointHash(violationCondition.orElseThrow()),
                pathAndCondition.condition()),
            violationCondition.orElseThrow());
      }
    }
    ImmutableListMultimap<HashAndOrigin, AbstractState> statePerProgramCounter =
        statePerProgramCounterBuilder.build();
    ImmutableList.Builder<StateAndPrecision> vcs = ImmutableList.builder();
    if (combineByHash) {
      for (HashAndOrigin hashAndOrigin : statePerProgramCounter.keySet()) {
        vcs.add(
            new StateAndPrecision(
                dcpa.getCombineViolationConditionsOperator()
                    .combineViolationConditionsAtSameProgramHash(
                        Optional.ofNullable(hashAndOrigin.origin()),
                        statePerProgramCounter.get(hashAndOrigin)),
                makeStartPrecision()));
      }
    } else {
      Precision p = makeStartPrecision();
      vcs.addAll(
          FluentIterable.from(statePerProgramCounter.values())
              .transform(s -> new StateAndPrecision(s, p)));
    }
    ImmutableList<StateAndPrecision> allVcs = vcs.build();
    if (allVcs.isEmpty()) {
      return ImmutableSet.of();
    }
    ImmutableMap<String, String> serialized = serialize(allVcs);
    return ImmutableSet.of(
        messageFactory.createViolationConditionMessage(block.getId(), status, serialized));
  }

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
    ImmutableList.Builder<ARGPath> paths = ImmutableList.builder();
    for (ARGState state : states) {
      paths.addAll(ARGUtils.getAllPaths(reachedSet, state));
    }
    return paths.build();
  }

  private Set<ArgPathAndCondition> computeViolationConditionStatesFromOrigin(
      Collection<@NonNull ARGState> state) {
    ImmutableSet.Builder<ArgPathAndCondition> relevantViolations = ImmutableSet.builder();
    for (ARGPath path : collectPaths(state)) {
      relevantViolations.add(new ArgPathAndCondition(path, null));
    }
    return relevantViolations.build();
  }

  private Set<ArgPathAndCondition> computeViolationConditionStatesFromBlockEnd(
      Collection<@NonNull ARGState> violations, Collection<@NonNull ARGState> conditions) {
    ImmutableSet.Builder<ArgPathAndCondition> relevantViolations = ImmutableSet.builder();
    for (ARGState violation : violations) {
      for (ARGPath path : collectPaths(ImmutableList.of(violation))) {
        for (ARGState condition : conditions) {
          relevantViolations.add(new ArgPathAndCondition(path, condition));
        }
      }
    }
    return relevantViolations.build();
  }

  private Set<ArgPathAndCondition> computeViolationConditionStates(
      Collection<@NonNull ARGState> violations) {
    ImmutableSet.Builder<ArgPathAndCondition> relevantViolations = ImmutableSet.builder();
    for (ARGState violation : violations) {
      BlockState condition =
          Objects.requireNonNull(AbstractStates.extractStateByType(violation, BlockState.class));
      ARGState violationState =
          (ARGState) Iterables.getOnlyElement(condition.getViolationConditions());
      for (ARGPath path : collectPaths(ImmutableList.of(violation))) {
        relevantViolations.add(new ArgPathAndCondition(path, violationState));
      }
    }
    return relevantViolations.build();
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
        return DssMessageProcessing.stop();
      }
      ImmutableList.Builder<StateAndPrecision> summariesWithPrecision = ImmutableList.builder();
      for (AbstractState finalState : result.getFinalLocationStates()) {
        summariesWithPrecision.add(
            new StateAndPrecision(finalState, reachedSet.getPrecision(finalState)));
      }
      return reportPostconditions(summariesWithPrecision.build());
    }

    ImmutableList.Builder<DssMessage> messages = ImmutableList.builder();
    if (!result.getFinalLocationStates().isEmpty()) {
      AbstractState startState = makeTopState(block.getFinalLocation());
      Precision startPrecision = makeStartPrecision();
      messages.add(
          messageFactory.createDssPostConditionMessage(
              block.getId(),
              status,
              serialize(ImmutableList.of(new StateAndPrecision(startState, startPrecision)))));
    }
    return messages.addAll(reportFirstViolationConditions(result.getAllViolations())).build();
  }

  private void appendTopToRelevantIfNecessary(String id) throws InterruptedException {
    // calculate for all new states but do not underapproximate
    if (preconditions.keySet().size() != block.getPredecessorIds().size()) {
      relevant.add(new StateAndPrecision(makeStartState(), makeStartPrecision()));
      return;
    }
    for (String k : preconditions.keySet()) {
      if (k.equals(id)) {
        continue;
      }
      if (preconditions.get(k).stream()
          .anyMatch(s -> dcpa.isMostGeneralBlockEntryState(s.state()))) {
        relevant.add(new StateAndPrecision(makeStartState(), makeStartPrecision()));
        return;
      }
    }
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
    storePreconditionTime.start();
    try {
      relevant.clear();
      logger.log(Level.INFO, "Running forward analysis with new precondition");
      resetStates();
      ImmutableList<@NonNull StateAndPrecision> deserializedStatesAndPrecisions =
          deserialize(pReceived);
      DssMessageProcessing processing = DssMessageProcessing.proceed();
      for (StateAndPrecision stateAndPrecision : deserializedStatesAndPrecisions) {
        processing =
            processing.merge(
                dcpa.getProceedOperator().processForward(stateAndPrecision.state()), true);
      }
      if (!processing.shouldProceed()) {
        return processing;
      }

      if (preconditions.get(pReceived.getSenderId()).isEmpty()) {
        preconditions.putAll(pReceived.getSenderId(), deserializedStatesAndPrecisions);
        relevant.addAll(deserializedStatesAndPrecisions);
        appendTopToRelevantIfNecessary(pReceived.getSenderId());
        return processing;
      }
      for (StateAndPrecision deserializedStateAndPrecision : deserializedStatesAndPrecisions) {
        boolean isRelevant = true;
        for (StateAndPrecision stateAndPrecision :
            ImmutableSet.copyOf(preconditions.get(pReceived.getSenderId()))) {
          if (dcpa.getCoverageOperator()
              .isSubsumed(
                  dcpa.reset(deserializedStateAndPrecision.state()), stateAndPrecision.state())) {
            preconditions.remove(pReceived.getSenderId(), stateAndPrecision);
          }
          if (isRelevant
              && dcpa.getCoverageOperator()
                  .isSubsumed(
                      stateAndPrecision.state(),
                      dcpa.reset(deserializedStateAndPrecision.state()))) {
            isRelevant = false;
          }
        }
        if (isRelevant) {
          relevant.add(deserializedStateAndPrecision);
        }
        preconditions.put(pReceived.getSenderId(), deserializedStateAndPrecision);
      }
      if (relevant.isEmpty()) {
        return DssMessageProcessing.stop();
      }

      appendTopToRelevantIfNecessary(pReceived.getSenderId());
      return processing;
    } finally {
      storePreconditionTime.stop();
      storePreconditionCount.inc();
    }
  }

  private ViolationWitness extractWitnessFromState(AbstractState state) {
    return Objects.requireNonNull(AbstractStates.extractStateByType(state, BlockState.class))
        .getWitness();
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
    storeViolationConditionTime.start();
    try {
      logger.log(Level.INFO, "Running forward analysis with respect to error condition");
      // merge all states into the reached set
      ImmutableList<StateAndPrecision> deserializedStates = deserialize(pNewViolationCondition);
      Set<ViolationWitness> oldVcs =
          transformedImmutableSetCopy(
              violationConditions.removeAll(pNewViolationCondition.getSenderId()),
              sap -> extractWitnessFromState(sap.state()));
      int equal = 0;
      for (StateAndPrecision stateAndPrecision : deserializedStates) {
        if (oldVcs.contains(extractWitnessFromState(stateAndPrecision.state()))) {
          equal++;
        }
        DssMessageProcessing current =
            dcpa.getProceedOperator().processBackward(stateAndPrecision.state());
        if (current.shouldProceed()) {
          violationConditions.put(pNewViolationCondition.getSenderId(), stateAndPrecision);
        }
      }
      if (violationConditions.get(pNewViolationCondition.getSenderId()).isEmpty()
          || equal == deserializedStates.size()) {
        return DssMessageProcessing.stop();
      }
      return DssMessageProcessing.proceed();
    } finally {
      storeViolationConditionTime.stop();
      storeViolationConditionCount.inc();
    }
  }

  /**
   * Adds a new abstract state to the known start states and execute the configured forward
   * analysis.
   *
   * @return All violations and/or abstractions that occurred while running the forward analysis.
   */
  public Collection<DssMessage> analyzePrecondition()
      throws SolverException, InterruptedException, CPAException {
    if (!containsViolationInsideBlock && violationConditions.isEmpty()) {
      return ImmutableSet.of();
    }
    analyzePreconditionTime.start();
    try {
      ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
      AnalysisResult result =
          analyzeViolationCondition(
              transformedImmutableListCopy(violationConditions.values(), v -> (ARGState) v.state()),
              true);
      if (!result.violationConditions().isEmpty()) {
        messages.addAll(reportViolationConditions(result.violationConditions()));
      }
      if (!result.summaries().isEmpty()) {
        messages.addAll(reportPostconditions(result.summaries()));
      }
      return messages.build();
    } finally {
      analyzePreconditionTime.stop();
      analyzePreconditionCount.inc();
    }
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
    relevant.clear();
    Collection<@NonNull StateAndPrecision> violations = violationConditions.get(pSenderId);
    if (violations.isEmpty()) {
      throw new IllegalArgumentException(
          "No violation condition found for sender ID: " + pSenderId);
    }
    analyzeViolationConditionTime.start();
    try {
      ImmutableList.Builder<DssMessage> messages = ImmutableList.builder();
      AnalysisResult result =
          analyzeViolationCondition(
              transformedImmutableListCopy(violations, v -> (ARGState) v.state()), false);
      if (!result.summaries().isEmpty()) {
        messages.addAll(reportPostconditions(result.summaries()));
      }
      if (!result.violationConditions().isEmpty()) {
        messages.addAll(reportViolationConditions(result.violationConditions()));
      }
      return messages.build();
    } finally {
      analyzeViolationConditionTime.stop();
      analyzeViolationConditionCount.inc();
    }
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
  private AnalysisResult analyzeViolationCondition(
      List<ARGState> violations, boolean checkOnlyRelevant)
      throws CPAException, InterruptedException {
    if (preconditions.isEmpty() && !block.isRoot()) {
      return new AnalysisResult(ImmutableList.of(), ImmutableSet.of());
    }

    boolean hasNonTrivialSummariesForEachPredecessor =
        !preconditions.isEmpty()
            && preconditions.keySet().stream()
                .allMatch(
                    k ->
                        preconditions.get(k).stream()
                            .anyMatch(sap -> !dcpa.isMostGeneralBlockEntryState(sap.state())));

    // unreachable block ends might be caused by underapproximating summaries
    // therefore, a new violation condition cannot ignore them.
    // create start states for the forward analysis.
    ImmutableSet.Builder<StateAndPrecision> startStates = ImmutableSet.builder();
    if (checkOnlyRelevant) {
      startStates.addAll(relevant);
    } else {
      if (!preconditions.values().isEmpty()) {
        for (StateAndPrecision sap : preconditions.values()) {
          if (hasNonTrivialSummariesForEachPredecessor
              && AbstractStates.extractStateByType(sap.state(), BlockState.class)
                  .hasNonTrivialSummaryForEachPredecessor()
              && dcpa.isMostGeneralBlockEntryState(sap.state())) {
            continue;
          }
          startStates.add(sap);
        }
      } else {
        startStates.add(new StateAndPrecision(makeStartState(), makeStartPrecision()));
      }
    }

    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableSet.Builder<ArgPathAndCondition> vcs = ImmutableSet.builder();

    boolean analyzedTrivial = false;
    ImmutableSet<StateAndPrecision> finalStartStates = startStates.build();
    for (StateAndPrecision stateAndPrecision : finalStartStates) {
      boolean isTrivial = dcpa.isMostGeneralBlockEntryState(stateAndPrecision.state());
      if (isTrivial && analyzedTrivial) {
        continue;
      }
      analyzedTrivial = analyzedTrivial || isTrivial;
      resetStates();
      reachedSet.clear();
      reachedSet.add(
          stateAndPrecision.state(),
          resetPrecisionsForEveryRun || isTrivial
              ? makeStartPrecision()
              : combinePrecisionIfPossible().orElse(stateAndPrecision.precision()));
      Objects.requireNonNull(
              AbstractStates.extractStateByType(stateAndPrecision.state(), BlockState.class))
          .setViolationConditions(violations);

      DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

      status = status.update(result.getStatus());

      if (block.isAbstractionPossible()) {
        if (!result.getFinalLocationStates().isEmpty()) {
          for (AbstractState summary : result.getFinalLocationStates()) {
            AbstractStates.extractStateByType(summary, BlockState.class)
                .setTopSummaryFromNonTrivialState(hasNonTrivialSummariesForEachPredecessor);
            summaries.add(new StateAndPrecision(summary, reachedSet.getPrecision(summary)));
          }
        }
        if (!result.getAllViolations().isEmpty()) {
          // pack all violations
          if (!checkOnlyRelevant || finalStartStates.size() == 1 || !isTrivial) {
            // this is true if we are in a backward analysis, or we only have one state to consider
            // or the state is non-trivial.
            // For trivial states, the same vc must have been sent already.
            vcs.addAll(computeViolationConditionStates(result.getViolationConditionViolations()));
          }
          if (containsViolationInsideBlock) {
            vcs.addAll(computeViolationConditionStatesFromOrigin(result.getTargetStates()));
          }
        }
      } else {
        // forward vcs
        vcs.addAll(
            computeViolationConditionStatesFromBlockEnd(
                result.getFinalLocationStates(), violations));
      }
    }
    return new AnalysisResult(summaries.build(), vcs.build());
  }

  private AbstractState makeTopState(CFANode pLocation) throws InterruptedException {
    return dcpa.getInitialState(pLocation, StateSpacePartition.getDefaultPartition());
  }

  private AbstractState makeStartState() throws InterruptedException {
    return makeTopState(block.getInitialLocation());
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

  public DistributedConfigurableProgramAnalysis getDcpa() {
    return dcpa;
  }
}
