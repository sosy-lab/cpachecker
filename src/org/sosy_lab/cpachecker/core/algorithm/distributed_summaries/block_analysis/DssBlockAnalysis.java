// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
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
import org.sosy_lab.cpachecker.cpa.path.SegmentedPaths;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class DssBlockAnalysis<
    PreconditionIndex extends DssIndexable, PostconditionIndex extends DssIndexable> {

  protected static final class ArgPathAndCondition {

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
          && Objects.equals(condition, other.condition())
          && Objects.equals(path.getFirstState(), other.path.getFirstState());
    }
  }

  protected record AnalysisComponents(
      Algorithm algorithm, ConfigurableProgramAnalysis cpa, ReachedSet reached) {}

  protected record AnalysisResult(
      Collection<StateAndPrecision> summaries, Set<ArgPathAndCondition> violationConditions) {}

  protected final DistributedConfigurableProgramAnalysis dcpa;
  protected final DssMessageFactory messageFactory;
  protected final Multimap<PreconditionIndex, @NonNull StateAndPrecision> preconditions;
  protected final Multimap<PostconditionIndex, @NonNull StateAndPrecision> violationConditions;

  protected final BlockNode block;
  protected final ReachedSet reachedSet;
  protected final Algorithm algorithm;

  protected AlgorithmStatus status;
  protected boolean containsViolationInsideBlock;

  protected final boolean combineByHash;

  protected final LogManager logger;

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
    status = AlgorithmStatus.SOUND_AND_PRECISE;
    messageFactory = pMessageFactory;
    AnalysisComponents parts =
        createBlockAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    // prepare dcpa and the algorithms
    algorithm = parts.algorithm();
    ConfigurableProgramAnalysis cpa = parts.cpa();
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
    combineByHash = pOptions.combineByHash();
  }

  public ImmutableMap<String, String> serializedPreconditions() {
    return serialize(ImmutableList.copyOf(preconditions.values()));
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

  protected Collection<DssMessage> reportPostconditions(
      Collection<@NonNull StateAndPrecision> summaries) {
    if (summaries.isEmpty()) {
      return ImmutableList.of();
    }

    summaries.forEach(sap-> sap.getBlockState().addHistory(block));
    return ImmutableList.of(
        messageFactory.createDssPostConditionMessage(
            block.getId(), status, serialize(ImmutableList.copyOf(summaries))));
  }

  protected Collection<DssMessage> reportFirstViolationConditions(Set<@NonNull ARGState> violations)
      throws CPAException, InterruptedException, SolverException {
    containsViolationInsideBlock = true;
    return reportViolationConditions(computeViolationConditionStatesFromOrigin(violations));
  }

  protected Collection<DssMessage> reportViolationConditions(
      Collection<ArgPathAndCondition> relevantViolations)
      throws InterruptedException, CPAException, SolverException {
    ImmutableListMultimap.Builder<Integer, AbstractState> statePerProgramCounterBuilder =
        ImmutableListMultimap.builder();
    for (ArgPathAndCondition pathAndCondition : relevantViolations) {
      Optional<AbstractState> violationCondition =
          dcpa.getViolationConditionOperator()
              .computeViolationCondition(
                  pathAndCondition.path(), Optional.ofNullable(pathAndCondition.condition()));
      if (violationCondition.isPresent()) {
        statePerProgramCounterBuilder.put(
            Objects.hash(
                pathAndCondition.condition(),
                dcpa.computeProgramPointHash(violationCondition.orElseThrow())),
            violationCondition.orElseThrow());
      }
    }
    ImmutableListMultimap<Integer, AbstractState> statePerProgramCounter =
        statePerProgramCounterBuilder.build();
    ImmutableList.Builder<StateAndPrecision> vcs = ImmutableList.builder();
    if (combineByHash) {
      for (Integer i : statePerProgramCounter.keySet()) {
        vcs.add(
            new StateAndPrecision(
                dcpa.getCombineViolationConditionsOperator()
                    .combineViolationConditionsAtSameProgramHash(statePerProgramCounter.get(i)),
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

  protected SegmentedPaths extractWitnessFromState(AbstractState state) {
    return Objects.requireNonNull(AbstractStates.extractStateByType(state, BlockState.class))
        .getWitness();
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
  protected ImmutableMap<String, String> serialize(
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
  public ImmutableList<@NonNull StateAndPrecision> deserialize(final DssMessage pMessage)
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
      if (pMessage.getType() == DssMessageType.POST_CONDITION) {
        state = dcpa.reset(state);
      }
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

  protected Set<ArgPathAndCondition> computeViolationConditionStatesFromOrigin(
      Collection<@NonNull ARGState> state) {
    ImmutableSet.Builder<ArgPathAndCondition> relevantViolations = ImmutableSet.builder();
    for (ARGPath path : collectPaths(state)) {
      relevantViolations.add(new ArgPathAndCondition(path, null));
    }
    return relevantViolations.build();
  }

  protected Set<ArgPathAndCondition> computeViolationConditionStates(
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

  protected AbstractState makeTopState(CFANode pLocation) throws InterruptedException {
    return dcpa.getInitialState(pLocation, StateSpacePartition.getDefaultPartition());
  }

  protected AbstractState makeStartState() throws InterruptedException {
    return makeTopState(block.getInitialLocation());
  }

  protected Precision makeStartPrecision() throws InterruptedException {
    return dcpa.getInitialPrecision(
        block.getInitialLocation(), StateSpacePartition.getDefaultPartition());
  }

  /**
   * Resets all preconditions to their initial state, i.e., the ARGState is wrapped in a new
   * ARGState without any parent.
   */
  protected void resetStates() {
    for (Entry<PreconditionIndex, StateAndPrecision> entry :
        ImmutableList.copyOf(preconditions.entries())) {
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

  /**
   * Executes the configured CPA algorithm on the block with the initial state and precision.
   *
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   * @throws SolverException thrown if solver runs into an error
   */
  public abstract Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException;

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
  public abstract DssMessageProcessing storePrecondition(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException;

  /**
   * Adds a new abstract state to the known violation conditions.
   *
   * @param pNewViolationCondition The new violation condition to add.
   * @return Whether the analysis should proceed.
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   * @throws SolverException thrown if solver runs into an error
   */
  public abstract DssMessageProcessing storeViolationCondition(
      DssViolationConditionMessage pNewViolationCondition)
      throws InterruptedException, SolverException;

  /**
   * Adds a new abstract state to the known start states and execute the configured forward
   * analysis.
   *
   * @return All violations and/or abstractions that occurred while running the forward analysis.
   */
  public abstract Collection<DssMessage> analyzePreconditions(String idFormLastUpdate)
      throws SolverException, InterruptedException, CPAException;

  /**
   * Analyzes the violation condition for the given sender ID. The violation condition is extracted
   * from the violation conditions stored via {@link
   * #storeViolationCondition(DssViolationConditionMessage)}
   *
   * @param idFormLastUpdate Sender ID of the violation-condition message to analyze.
   * @return The messages resulting from the analysis of the violation condition.
   */
  public abstract Collection<DssMessage> analyzeViolationConditions(String idFormLastUpdate)
      throws SolverException, InterruptedException, CPAException;
}
