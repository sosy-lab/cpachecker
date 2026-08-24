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

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFormat;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
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
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The analysis of a single {@link BlockNode} within the distributed-summary-synthesis algorithm.
 *
 * <p>An instance owns the CPA, the algorithm and the reached set of one block and knows how to
 * explore the block, how to (de)serialize abstract states and which explored states become messages
 * for other blocks. What it does <em>not</em> decide is how the block reacts to the messages it
 * receives; that is delegated to two collaborators:
 *
 * <ul>
 *   <li>a {@link DssPreconditionHandler} for the postconditions received from predecessor blocks,
 *       and
 *   <li>a {@link DssViolationConditionHandler} for the violation conditions received from successor
 *       blocks.
 * </ul>
 *
 * <p>Both are chosen by {@link DssAnalysisOptions#getBlockAnalysisType()}, so the behavior of a
 * block is assembled from configuration rather than fixed by a class hierarchy.
 */
public final class DssBlockAnalysis {

  private record AnalysisComponents(
      Algorithm algorithm, ConfigurableProgramAnalysis cpa, ReachedSet reached) {}

  private record ViolationConditionProgramPoint(
      Optional<ARGState> previousCondition, Object programPoint) {}

  private final BlockNode block;
  private final LogManager logger;
  private final DssMessageFactory messageFactory;
  private final DssAnalysisOptions options;
  private final DssSingleWorkerStatistics workerStats;

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final Algorithm algorithm;
  private final ReachedSet reachedSet;

  private final DssPreconditionHandler preconditions;
  private final DssViolationConditionHandler violationConditionHandler;

  private AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
  private boolean containsViolationInsideBlock;

  public DssBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCfa,
      Specification pSpecification,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      ShutdownManager pShutdownManager,
      DssSingleWorkerStatistics pWorkerStats)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    block = pBlock;
    logger = pLogger;
    messageFactory = pMessageFactory;
    options = pOptions;
    workerStats = pWorkerStats;

    AnalysisComponents parts =
        createBlockAlgorithm(
            pLogger, pSpecification, pCfa, pConfiguration, pShutdownManager, pBlock);
    algorithm = parts.algorithm();
    ConfigurableProgramAnalysis cpa = parts.cpa();

    dcpa =
        DssFactory.distribute(
            cpa,
            pBlock,
            pCfa,
            pConfiguration,
            pOptions,
            pMessageFactory,
            pLogger,
            pShutdownManager.getNotifier());
    assert dcpa != null : "Distribution of " + cpa.getClass().getSimpleName() + " not implemented.";

    reachedSet = parts.reached();
    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    reachedSet.clear();

    // Register dcpa-level statistics with the worker stats object.
    if (dcpa instanceof DistributedARGCPA arg
        && arg.getWrappedCPA() instanceof DistributedCompositeCPA composite) {
      pWorkerStats.setDcpaStatistics(composite.getStatistics());
    }

    // Assembled last: the handlers use the services above, which are all initialized by now.
    DssBlockAnalysisType type = pOptions.getBlockAnalysisType();
    violationConditionHandler = type.createViolationConditionHandler(this);
    preconditions = type.createPreconditionHandler(this);
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
   * Executes the configured CPA algorithm on the block with the initial state and precision.
   *
   * @return Important messages for other blocks.
   */
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    return preconditions.runInitialAnalysis();
  }

  /**
   * Adds a new precondition to the known preconditions. The method checks whether the new
   * precondition is already covered by an existing one. If this is the case, the new precondition
   * is discarded and the analysis will not proceed. Otherwise, the new precondition is added and
   * the analysis will proceed.
   *
   * @param pReceived The new precondition to add.
   * @return Whether the analysis should proceed.
   */
  public DssMessageProcessing storePrecondition(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    return preconditions.store(pReceived);
  }

  /**
   * Re-explores the block after {@link #storePrecondition} asked the analysis to proceed.
   *
   * @return All violations and/or abstractions that occurred while running the forward analysis.
   */
  public Collection<DssMessage> analyzePreconditions()
      throws SolverException, InterruptedException, CPAException {
    return preconditions.analyze();
  }

  /**
   * Adds new abstract states to the known violation conditions.
   *
   * @param pReceived The new violation conditions to add.
   * @return Whether the analysis should proceed.
   */
  public DssMessageProcessing storeViolationCondition(DssViolationConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    DssMessageProcessing processing = violationConditionHandler.store(pReceived);
    if (processing.shouldProceed()) {
      preconditions.violationConditionsChanged();
    }
    return processing;
  }

  /**
   * Analyzes the violation conditions received from the given block.
   *
   * @param pSenderId Sender ID of the violation-condition message to analyze.
   * @return The messages resulting from the analysis of the violation condition.
   */
  public Collection<DssMessage> analyzeViolationConditions(String pSenderId)
      throws SolverException, InterruptedException, CPAException {
    return preconditions.analyzeFor(pSenderId);
  }

  public ImmutableMap<String, String> serializedPreconditions() {
    return serialize(preconditions.getKnownPreconditions());
  }

  public DistributedConfigurableProgramAnalysis getDcpa() {
    return dcpa;
  }

  DssSingleWorkerStatistics statistics() {
    return workerStats;
  }

  /** The most general state at the given location, i.e., the one that constrains nothing. */
  private AbstractState makeTopState(CFANode pLocation) throws InterruptedException {
    return dcpa.getInitialState(pLocation, StateSpacePartition.getDefaultPartition());
  }

  /**
   * The unconstrained state at the block entry, with this block already recorded in its history.
   *
   * @param ignoreCallstackIfAvailable whether the callstack CPA may ignore its transfer while the
   *     state is built, so that the block entry is not tied to one call context
   */
  AbstractState makeStartState(boolean ignoreCallstackIfAvailable) throws InterruptedException {
    AbstractState state;
    disableCallstackIfAvailable(ignoreCallstackIfAvailable);
    try {
      state = makeTopState(block.getInitialLocation());
    } finally {
      disableCallstackIfAvailable(false);
    }
    blockStateOf(state).addHistory(block);
    return state;
  }

  Precision makeStartPrecision() throws InterruptedException {
    return dcpa.getInitialPrecision(
        block.getInitialLocation(), StateSpacePartition.getDefaultPartition());
  }

  static BlockState blockStateOf(AbstractState pState) {
    return Objects.requireNonNull(AbstractStates.extractStateByType(pState, BlockState.class));
  }

  SegmentedPaths witnessOf(AbstractState pState) {
    return blockStateOf(pState).getWitness();
  }

  /** Runs the proceed operator over all received states and merges the outcome. */
  DssMessageProcessing shouldProceedForward(Collection<@NonNull StateAndPrecision> pReceived)
      throws InterruptedException, SolverException {
    DssMessageProcessing processing = DssMessageProcessing.proceed();
    for (StateAndPrecision stateAndPrecision : pReceived) {
      processing =
          processing.merge(
              dcpa.getProceedOperator().processForward(stateAndPrecision.state()), true);
    }
    return processing;
  }

  boolean shouldProceedBackward(AbstractState pState) throws InterruptedException, SolverException {
    return dcpa.getProceedOperator().processBackward(pState).shouldProceed();
  }

  /** Combines the precisions of the received preconditions with the one used so far. */
  Precision combinePrecisions(Collection<@NonNull StateAndPrecision> pReceived)
      throws InterruptedException {
    return dcpa.getCombinePrecisionOperator()
        .combine(transformedImmutableListCopy(pReceived, StateAndPrecision::precision));
  }

  /**
   * Resets all given preconditions to their initial state, i.e., the ARGState is wrapped in a new
   * ARGState without any parent.
   */
  <K> void resetStates(Multimap<K, @NonNull StateAndPrecision> pPreconditions) {
    for (Entry<K, StateAndPrecision> entry : ImmutableList.copyOf(pPreconditions.entries())) {
      pPreconditions.remove(entry.getKey(), entry.getValue());
      pPreconditions.put(
          entry.getKey(),
          new StateAndPrecision(
              dcpa.reset(entry.getValue().state()), entry.getValue().precision()));
    }
  }

  /**
   * Counts how many of {@code pStates} are equal to at least one state in {@code pCandidates}.
   *
   * @return a number between 0 and {@code pStates.size()}
   */
  int countCovered(
      Collection<@NonNull StateAndPrecision> pStates,
      Collection<@NonNull StateAndPrecision> pCandidates)
      throws CPAException, InterruptedException {
    // TODO rather inefficient
    int covered = 0;
    for (StateAndPrecision state : pStates) {
      for (StateAndPrecision candidate : pCandidates) {
        if (dcpa.getCoverageOperator().areStatesEqual(state.state(), candidate.state())) {
          covered++;
          break;
        }
      }
    }
    return covered;
  }

  /**
   * Removes duplicates from the given states and precisions, i.e., the returned list contains
   * exactly one representative of every class of {@link StateAndPrecision} whose states are equal
   * according to {@link CoverageOperator#areStatesEqual}.
   *
   * <p>Only the states decide whether two entries are duplicates. The precision of a discarded
   * entry is lost, so the caller has to combine the precisions beforehand (see {@link
   * #combinePrecisions(Collection)}) if all of them have to be kept.
   *
   * @param pStatesAndPrecisions The states and precisions to deduplicate.
   * @return The first entry of every class of equal states, in the order of {@code
   *     pStatesAndPrecisions}.
   */
  ImmutableList<StateAndPrecision> deduplicateStatesAndPrecisions(
      Iterable<@NonNull StateAndPrecision> pStatesAndPrecisions)
      throws CPAException, InterruptedException {
    return deduplicate(pStatesAndPrecisions, StateAndPrecision::state);
  }

  /**
   * Removes all elements whose state is equal to the state of an earlier element, according to
   * {@link CoverageOperator#areStatesEqual}.
   *
   * <p>Equal states are at the same program point and, thus, have the same program-point hash. The
   * elements are therefore grouped by that hash first, and only elements within the same group are
   * compared with the (potentially expensive) coverage operator.
   *
   * @param pElements The elements to deduplicate.
   * @param pStateOf Extracts the state that identifies an element.
   * @return The first element of every class of equal states, in the order of {@code pElements}.
   */
  private <T> ImmutableList<T> deduplicate(
      Iterable<@NonNull T> pElements, Function<T, AbstractState> pStateOf)
      throws CPAException, InterruptedException {
    CoverageOperator coverage = dcpa.getCoverageOperator();
    ListMultimap<Object, AbstractState> representativesPerProgramPoint = ArrayListMultimap.create();
    ImmutableList.Builder<T> deduplicated = ImmutableList.builder();
    for (T element : pElements) {
      AbstractState state = pStateOf.apply(element);
      List<AbstractState> representatives =
          representativesPerProgramPoint.get(dcpa.computeProgramPointId(state));
      boolean isDuplicate = false;
      for (AbstractState representative : representatives) {
        if (state == representative || coverage.areStatesEqual(state, representative)) {
          isDuplicate = true;
          break;
        }
      }
      if (!isDuplicate) {
        // ArrayListMultimap#get returns a view that writes through to the multimap.
        representatives.add(state);
        deduplicated.add(element);
      }
    }
    return deduplicated.build();
  }

  /** Whether every state in {@code pStates} is covered by some state in {@code pCandidates}. */
  boolean allCovered(
      Collection<@NonNull StateAndPrecision> pStates,
      Collection<@NonNull StateAndPrecision> pCandidates)
      throws CPAException, InterruptedException {
    return countCovered(pStates, pCandidates) == pStates.size();
  }

  /**
   * Explores the block once from the given precondition, without any violation condition attached.
   *
   * <p>This run is intentionally not counted in the block-analysis statistics, which only track the
   * re-analyses triggered by incoming messages.
   */
  DssBlockAnalysisResult runInitialBlockAnalysis(AbstractState pPrecondition, Precision pPrecision)
      throws CPAException, InterruptedException {
    reachedSet.clear();
    reachedSet.add(pPrecondition, pPrecision);
    DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet);
    status = status.update(result.getStatus());
    return result;
  }

  /** Explores the block once from the given precondition under the given violation conditions. */
  DssBlockAnalysisResult runBlockAnalysis(
      AbstractState pPrecondition,
      Precision pPrecision,
      Collection<AbstractState> pViolationConditions)
      throws CPAException, InterruptedException {
    reachedSet.clear();
    reachedSet.add(pPrecondition, pPrecision);
    blockStateOf(pPrecondition).setViolationConditions(ImmutableList.copyOf(pViolationConditions));
    try {
      workerStats.getBlockAnalysisTimer().start();
      DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet);
      status = status.update(result.getStatus());
      return result;
    } finally {
      workerStats.getBlockAnalysisTimer().stop();
      workerStats.getBlockAnalysisCounter().inc();
    }
  }

  /**
   * The states at the final location of the block, paired with the precision they were found in.
   */
  ImmutableList<StateAndPrecision> summariesOf(DssBlockAnalysisResult pResult) {
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    for (ARGState summary : pResult.getFinalLocationStates()) {
      summaries.add(new StateAndPrecision(summary, reachedSet.getPrecision(summary)));
    }
    return summaries.build();
  }

  Collection<DssMessage> reportPostconditions(Collection<@NonNull StateAndPrecision> pSummaries) {
    if (pSummaries.isEmpty()) {
      return ImmutableList.of();
    }
    return ImmutableList.of(
        messageFactory.createDssPostConditionMessage(
            block.getId(), status, serialize(ImmutableList.copyOf(pSummaries))));
  }

  /**
   * Reports that the end of this block is unreachable, so successors must not be entered through
   * it.
   *
   * <p>Successors recognize this from a flag on the message rather than from the states it carries,
   * which keeps a genuine top postcondition (see {@link #makeTopState}) distinguishable from an
   * unreachable block end.
   */
  Collection<DssMessage> reportUnreachableBlockEnd() {
    return ImmutableList.of(
        messageFactory.createDssUnreachableBlockEndMessage(block.getId(), status));
  }

  /**
   * Reports violations that originate in this block and records that this block is known to contain
   * a violation.
   */
  Collection<DssMessage> reportFirstViolationConditions(Set<@NonNull ARGState> pViolations)
      throws CPAException, InterruptedException, SolverException {
    containsViolationInsideBlock = true;
    return reportViolationConditions(pathsFromOrigin(pViolations));
  }

  Collection<DssMessage> reportViolationConditions(
      Collection<ArgPathAndCondition> pRelevantViolations)
      throws InterruptedException, CPAException, SolverException {
    ImmutableListMultimap.Builder<ViolationConditionProgramPoint, AbstractState>
        statePerProgramCounterBuilder = ImmutableListMultimap.builder();
    for (ArgPathAndCondition pathAndCondition : pRelevantViolations) {
      Optional<AbstractState> violationCondition =
          dcpa.getViolationConditionOperator()
              .computeViolationCondition(
                  pathAndCondition.path(), Optional.ofNullable(pathAndCondition.condition()));
      Preconditions.checkState(
          violationCondition.isPresent(),
          "The analysis found a feasible counterexample "
              + "which could not be reestablished with the violation-condition operator.");
      statePerProgramCounterBuilder.put(
          new ViolationConditionProgramPoint(
              Optional.ofNullable(pathAndCondition.condition()),
              dcpa.computeProgramPointId(violationCondition.orElseThrow())),
          violationCondition.orElseThrow());
    }
    ImmutableListMultimap<ViolationConditionProgramPoint, AbstractState> statePerProgramCounter =
        statePerProgramCounterBuilder.build();
    ImmutableList.Builder<StateAndPrecision> vcs = ImmutableList.builder();
    if (options.combineViolationConditionsByHash()) {
      for (ViolationConditionProgramPoint programPoint : statePerProgramCounter.keySet()) {
        vcs.add(
            new StateAndPrecision(
                dcpa.getCombineViolationConditionsOperator()
                    .combineViolationConditionsAtSameProgramHash(
                        statePerProgramCounter.get(programPoint)),
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
    return ImmutableSet.of(
        messageFactory.createViolationConditionMessage(block.getId(), status, serialize(allVcs)));
  }

  /** All ARG paths reaching the given states, without an originating violation condition. */
  Set<ArgPathAndCondition> pathsFromOrigin(Collection<@NonNull ARGState> pStates) {
    ImmutableSet.Builder<ArgPathAndCondition> relevantViolations = ImmutableSet.builder();
    for (ARGPath path : collectPaths(pStates)) {
      relevantViolations.add(new ArgPathAndCondition(path, null));
    }
    return relevantViolations.build();
  }

  /**
   * All ARG paths reaching the given states, each paired with the violation condition that the
   * corresponding {@link BlockState} was analyzed under.
   */
  Set<ArgPathAndCondition> pathsWithCondition(Collection<@NonNull ARGState> pViolations) {
    ImmutableSet.Builder<ArgPathAndCondition> relevantViolations = ImmutableSet.builder();
    for (ARGState violation : pViolations) {
      ARGState violationState =
          (ARGState) Iterables.getOnlyElement(blockStateOf(violation).getViolationConditions());
      for (ARGPath path : collectPaths(ImmutableList.of(violation))) {
        relevantViolations.add(new ArgPathAndCondition(path, violationState));
      }
    }
    return relevantViolations.build();
  }

  private Collection<ARGPath> collectPaths(Iterable<@NonNull ARGState> pStates) {
    ImmutableList.Builder<ARGPath> paths = ImmutableList.builder();
    for (ARGState state : pStates) {
      paths.addAll(ARGUtils.getAllPaths(reachedSet, state));
    }
    return paths.build();
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
  ImmutableMap<String, String> serialize(
      final List<@NonNull StateAndPrecision> pStatesAndPrecisions) {
    ContentBuilder serializedContent = ContentBuilder.builder();
    serializedContent.put(
        DssMessageFormat.MULTIPLE_STATES_KEY,
        Integer.toString(pStatesAndPrecisions.size()));
    int totalStateSize = 0;
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
        totalStateSize += contents.getKey().length() + contents.getValue().length();
      }
      serializedContent.popLevel();
    }
    workerStats.getSerializedStatesSizeStats().setNextValue(totalStateSize);
    return serializedContent.build();
  }

  LogManager getLogger() {
    return logger;
  }

  boolean containsViolationInsideBlock() {
    return containsViolationInsideBlock;
  }

  DssAnalysisOptions getOptions() {
    return options;
  }

  DssViolationConditionHandler getViolationConditionHandler() {
    return violationConditionHandler;
  }

  DssPreconditionHandler getPreconditions() {
    return preconditions;
  }

  public BlockNode getBlock() {
    return block;
  }

  /**
   * The method restores a list of states and precisions from a DssMessage. In general, it should
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

  private void disableCallstackIfAvailable(boolean ignoreCallstack) {
    Optional.ofNullable(CPAs.retrieveCPA(dcpa, DistributedCallstackCPA.class))
        .ifPresent(c -> c.setIgnoreTransfer(ignoreCallstack));
  }
}
