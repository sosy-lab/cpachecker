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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.PrecisionCoverageOperator;
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
 * <p>An instance owns the CPA, the algorithm and the reached set of one block, knows how to run the
 * algorithm once, how to (de)serialize abstract states and which explored states become messages
 * for other blocks. What it does <em>not</em> decide is how the block reacts to the messages it
 * receives; that is delegated to three collaborators:
 *
 * <ul>
 *   <li>a {@link DssPreconditionHandler} that remembers the postconditions received from
 *       predecessor blocks,
 *   <li>a {@link DssViolationConditionHandler} that remembers the violation conditions received
 *       from successor blocks, and
 *   <li>a {@link DssExplorationEngine} that turns what the two handlers hold into CPA runs.
 * </ul>
 *
 * <p>All three are chosen by {@link DssAnalysisOptions#getBlockAnalysisType()}, so the behavior of
 * a block is assembled from configuration rather than fixed by a class hierarchy. Publishing what
 * an engine found stays here, so that every engine reports its results the same way.
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
  private final DssExplorationEngine engine;

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

    // Assembled last: the components use the services above, which are all initialized by now.
    DssBlockAnalysisComponents components = pOptions.getBlockAnalysisType().createComponents(this);
    preconditions = components.preconditions();
    violationConditionHandler = components.violationConditions();
    engine = components.engine();
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
    AnalysisResult round = engine.exploreInitially();
    if (!round.violationConditions().isEmpty()) {
      // the initial run explores the block without any violation condition attached, so every
      // violation it finds originates inside this block
      containsViolationInsideBlock = true;
    }
    return messagesFor(round);
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
   * Adds new abstract states to the known violation conditions.
   *
   * @param pReceived The new violation conditions to add.
   * @return Whether the analysis should proceed.
   */
  public DssMessageProcessing storeViolationCondition(DssViolationConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    return violationConditionHandler.store(pReceived);
  }

  /**
   * Re-explores the block after {@link #storePrecondition} or {@link #storeViolationCondition}
   * asked the analysis to proceed. The exploration always reads everything the handlers hold, not
   * only what the triggering messages brought.
   *
   * @param pViolationConditionsChanged whether {@link #storeViolationCondition} asked to proceed
   *     since the last exploration
   * @return All violations and/or abstractions that occurred while exploring the block.
   */
  public Collection<DssMessage> analyze(boolean pViolationConditionsChanged)
      throws SolverException, InterruptedException, CPAException {
    return messagesFor(engine.explore(pViolationConditionsChanged));
  }

  /**
   * Publishes what one round of exploring the block found: the violating paths go to the
   * predecessor blocks, and the postcondition -- or the explicit signal that there is none -- goes
   * to the successor blocks.
   */
  private Collection<DssMessage> messagesFor(AnalysisResult pRound)
      throws CPAException, InterruptedException, SolverException {
    ImmutableList.Builder<DssMessage> messages = ImmutableList.builder();
    if (!pRound.violationConditions().isEmpty()) {
      try {
        workerStats.getViolationConditionTimer().start();
        workerStats.getViolationConditionCounter().add(pRound.violationConditions().size());
        messages.addAll(reportViolationConditions(pRound.violationConditions()));
      } finally {
        workerStats.getViolationConditionTimer().stop();
      }
    }
    if (pRound.blockEndUnreachable()) {
      messages.addAll(reportUnreachableBlockEnd());
    } else {
      messages.addAll(reportPostconditions(pRound.summaries(), pRound.retractedContexts()));
    }
    return messages.build();
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
    return state;
  }

  Precision makeStartPrecision() throws InterruptedException {
    return dcpa.getInitialPrecision(
        block.getInitialLocation(), StateSpacePartition.getDefaultPartition());
  }

  static BlockState blockStateOf(AbstractState pState) {
    return Objects.requireNonNull(AbstractStates.extractStateByType(pState, BlockState.class));
  }

  /**
   * The given precondition, with this block recorded at the end of its history.
   *
   * <p>The history is appended in place, so this must only be called on a state that nobody else
   * holds yet, i.e., on a freshly created or freshly deserialized one.
   *
   * @see #withBlockInHistory(Collection) for the rationale of who records the history
   */
  AbstractState withBlockInHistory(AbstractState pState) {
    blockStateOf(pState).addHistory(block);
    return pState;
  }

  /**
   * The given preconditions, each with this block recorded at the end of its history.
   *
   * <p>Called by the receiver of a postcondition rather than by the sender before it serializes
   * (see {@link #reportPostconditions(Collection, ImmutableList)}), so that a block ends up in the
   * history of the preconditions it receives itself. A path-based receiver needs exactly that to
   * tell a repeat visit of a cycle apart from one reached via a genuinely new predecessor.
   */
  ImmutableList<@NonNull StateAndPrecision> withBlockInHistory(
      Collection<@NonNull StateAndPrecision> pStates) {
    return transformedImmutableListCopy(
        pStates,
        stateAndPrecision ->
            new StateAndPrecision(
                withBlockInHistory(stateAndPrecision.state()), stateAndPrecision.precision()));
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
   * Whether every state of {@code pStates} is covered by some state of {@code pCandidates},
   * regardless of the precisions (compare {@link #allCovered}).
   */
  boolean allStatesCovered(
      Collection<@NonNull StateAndPrecision> pStates,
      Collection<@NonNull StateAndPrecision> pCandidates)
      throws CPAException, InterruptedException {
    CoverageOperator coverage = dcpa.getCoverageOperator();
    try {
      workerStats.getCoverageTimer().start();
      for (StateAndPrecision state : pStates) {
        boolean covered = false;
        for (StateAndPrecision candidate : pCandidates) {
          workerStats.getCoverageCounter().inc();
          if (coverage.isSubsumed(state.state(), candidate.state())) {
            covered = true;
            break;
          }
        }
        if (!covered) {
          return false;
        }
      }
      return true;
    } finally {
      workerStats.getCoverageTimer().stop();
    }
  }

  /**
   * Whether an analysis run with the combined precision of {@code pStrong} is strictly more precise
   * than one with the combined precision of {@code pWeak}.
   */
  boolean precisionStrictlyStronger(
      Collection<@NonNull StateAndPrecision> pStrong, Collection<@NonNull StateAndPrecision> pWeak)
      throws InterruptedException {
    Precision strong = combinePrecisions(pStrong);
    Precision weak = combinePrecisions(pWeak);
    PrecisionCoverageOperator coverage = dcpa.getPrecisionCoverageOperator();
    return coverage.isSubsumed(weak, strong) && !coverage.isSubsumed(strong, weak);
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
   * Counts how many of {@code pStates} are covered by at least one state in {@code pCandidates}.
   *
   * <p>This is a one-way check, i.e. it uses {@link CoverageOperator#isSubsumed} and not {@link
   * CoverageOperator#areStatesEqual}. A strictly more general candidate has to count as a cover;
   * otherwise a precondition whose callstack is only partially known (see {@link
   * org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState}) could never cover a precondition with
   * a fully known callstack, because {@link CoverageOperator#areStatesEqual} also requires the
   * reverse direction, which {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack
   * .CallstackStateCoverageOperator} rejects by design.
   *
   * <p>A candidate only covers a state if its precision covers the state's precision as well (see
   * {@link PrecisionCoverageOperator}). Otherwise the analysis of the candidate tracked less than
   * what the state asks for, so its result is not the one that the state would have produced.
   *
   * <p>This answers "does {@code pStates} need to be analyzed, given that {@code pCandidates} has
   * been?", not "did the state set change?". Coverage is not a decision procedure for the latter:
   * since every state only needs <em>some</em> partner, mutual coverage of two sets only says that
   * their disjunctions are equivalent, so a set that gains a strictly stronger state still counts
   * as covered in both directions. Use {@link #statesEqual} to detect a change.
   *
   * @return a number between 0 and {@code pStates.size()}
   */
  int countCovered(
      Collection<@NonNull StateAndPrecision> pStates,
      Collection<@NonNull StateAndPrecision> pCandidates)
      throws CPAException, InterruptedException {
    // TODO rather inefficient
    int covered = 0;
    try {
      workerStats.getCoverageTimer().start();
      for (StateAndPrecision state : pStates) {
        for (StateAndPrecision candidate : pCandidates) {
          workerStats.getCoverageCounter().inc();
          if (isCovered(state, candidate)) {
            covered++;
            break;
          }
        }
      }
    } finally {
      workerStats.getCoverageTimer().stop();
    }
    return covered;
  }

  /**
   * Whether both the state and the precision of {@code pState} are covered by {@code pCandidate}.
   */
  private boolean isCovered(StateAndPrecision pState, StateAndPrecision pCandidate)
      throws CPAException, InterruptedException {
    return dcpa.getPrecisionCoverageOperator()
            .isSubsumed(pState.precision(), pCandidate.precision())
        && dcpa.getCoverageOperator().isSubsumed(pState.state(), pCandidate.state());
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
    return deduplicate(pElements, pStateOf, false);
  }

  private <T> ImmutableList<T> deduplicate(
      Iterable<@NonNull T> pElements, Function<T, AbstractState> pStateOf, boolean pSyntactically)
      throws CPAException, InterruptedException {
    CoverageOperator coverage = dcpa.getCoverageOperator();
    ListMultimap<Object, AbstractState> representativesPerProgramPoint = ArrayListMultimap.create();
    ImmutableList.Builder<T> deduplicated = ImmutableList.builder();
    try {
      workerStats.getCoverageTimer().start();
      for (T element : pElements) {
        AbstractState state = pStateOf.apply(element);
        List<AbstractState> representatives =
            representativesPerProgramPoint.get(dcpa.computeProgramPointId(state));
        boolean isDuplicate = false;
        for (AbstractState representative : representatives) {
          workerStats.getCoverageCounter().inc();
          if (state == representative
              || (pSyntactically
                  ? coverage.areStatesSyntacticallyEqual(state, representative)
                  : coverage.areStatesEqual(state, representative))) {
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
    } finally {
      workerStats.getCoverageTimer().stop();
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
   * Whether {@code pStates1} and {@code pStates2} contain the same set of states, i.e., every state
   * on either side has an equal state (per {@link CoverageOperator#areStatesEqual}) on the other
   * side.
   *
   * <p>This matters wherever the state set drives further exploration -- see {@link
   * AlwaysReplacePreconditionHandler} and {@link AlwaysReplaceViolationConditionHandler}, which
   * have to detect that a set gained or lost a state, not only that its states are still covered.
   *
   * <p>{@link CoverageOperator#areStatesEqual} is symmetric, so one pass over the pairs decides
   * both directions. Asking {@link #allCovered} once per direction instead evaluates every pair
   * twice, and a pair can cost a solver query.
   */
  boolean statesEqual(
      Collection<@NonNull StateAndPrecision> pStates1,
      Collection<@NonNull StateAndPrecision> pStates2)
      throws CPAException, InterruptedException {
    return statesEqual(pStates1, pStates2, false);
  }

  /**
   * Whether the two sets of violation conditions are the same. A violation condition is built from
   * the edges of a path, so the same path yields the same formula and the sets can be compared by
   * their representation, which spares a solver query per pair.
   */
  boolean violationConditionsEqual(
      Collection<@NonNull StateAndPrecision> pStates1,
      Collection<@NonNull StateAndPrecision> pStates2)
      throws CPAException, InterruptedException {
    return statesEqual(pStates1, pStates2, options.useSyntacticViolationConditionEquality());
  }

  /**
   * Whether two violation conditions are equal in the sense of {@link
   * #deduplicateViolationConditions}, i.e., one of them can be explored for both.
   */
  boolean isSameViolationCondition(
      @NonNull StateAndPrecision pCondition, @NonNull StateAndPrecision pOther)
      throws CPAException, InterruptedException {
    if (pCondition == pOther) {
      return true;
    }
    if (!dcpa.computeProgramPointId(pCondition.state())
        .equals(dcpa.computeProgramPointId(pOther.state()))) {
      return false;
    }
    CoverageOperator coverage = dcpa.getCoverageOperator();
    try {
      workerStats.getCoverageTimer().start();
      workerStats.getCoverageCounter().inc();
      return options.useSyntacticViolationConditionEquality()
          ? coverage.areStatesSyntacticallyEqual(pCondition.state(), pOther.state())
          : coverage.areStatesEqual(pCondition.state(), pOther.state());
    } finally {
      workerStats.getCoverageTimer().stop();
    }
  }

  /** Like {@link #deduplicateStatesAndPrecisions} for violation conditions. */
  ImmutableList<StateAndPrecision> deduplicateViolationConditions(
      Iterable<@NonNull StateAndPrecision> pStatesAndPrecisions)
      throws CPAException, InterruptedException {
    return deduplicate(
        pStatesAndPrecisions,
        StateAndPrecision::state,
        options.useSyntacticViolationConditionEquality());
  }

  private boolean statesEqual(
      Collection<@NonNull StateAndPrecision> pStates1,
      Collection<@NonNull StateAndPrecision> pStates2,
      boolean pSyntactically)
      throws CPAException, InterruptedException {
    CoverageOperator coverage = dcpa.getCoverageOperator();
    ImmutableList<StateAndPrecision> states2 = ImmutableList.copyOf(pStates2);
    boolean[] matchedInStates2 = new boolean[states2.size()];
    for (StateAndPrecision state1 : pStates1) {
      boolean matched = false;
      for (int i = 0; i < states2.size(); i++) {
        if (matched && matchedInStates2[i]) {
          // comparing them tells us nothing new: this state is already matched, and so is the
          // candidate. The comparison itself can cost a solver query, so skip it.
          continue;
        }
        if (pSyntactically
            ? coverage.areStatesSyntacticallyEqual(state1.state(), states2.get(i).state())
            : coverage.areStatesEqual(state1.state(), states2.get(i).state())) {
          matched = true;
          matchedInStates2[i] = true;
        }
      }
      if (!matched) {
        return false;
      }
    }
    for (boolean matched : matchedInStates2) {
      if (!matched) {
        return false;
      }
    }
    return true;
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
   *
   * <p>Every state at the final location is a summary, including those that already have an ARG
   * successor: the ghost edge is traversed whenever the block end is reached, so a successor is no
   * proof that the end was published before. Suppressing a reachable block end turns into an
   * unreachable-block-end message to the successors, i.e. a wrong proof.
   */
  ImmutableList<StateAndPrecision> summariesOf(DssBlockAnalysisResult pResult) {
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    for (ARGState summary : pResult.getFinalLocationStates()) {
      summaries.add(new StateAndPrecision(summary, reachedSet.getPrecision(summary)));
    }
    return summaries.build();
  }

  /**
   * Publishes the given postconditions together with the contexts of this block that no longer
   * produce one (see {@link AnalysisResult#retractedContexts()}). Nothing is published if both are
   * empty.
   */
  private Collection<DssMessage> reportPostconditions(
      Collection<@NonNull StateAndPrecision> pSummaries,
      ImmutableList<BlockGraphPath> pRetractedContexts) {
    if (pSummaries.isEmpty() && pRetractedContexts.isEmpty()) {
      return ImmutableList.of();
    }

    // History is recorded by the receiver (see DssPreconditionHandler#store implementations), not
    // here: a path-based receiver needs its own block already in a precondition's history to tell a
    // repeat visit of a cycle apart from one reached via a genuinely new predecessor.
    return ImmutableList.of(
        messageFactory.createDssPostConditionMessage(
            block.getId(),
            status,
            serialize(ImmutableList.copyOf(pSummaries)),
            pRetractedContexts));
  }

  /**
   * Reports that the end of this block is unreachable, so successors must not be entered through
   * it.
   *
   * <p>Successors recognize this from a flag on the message rather than from the states it carries,
   * which keeps a genuine top postcondition (see {@link #makeTopState}) distinguishable from an
   * unreachable block end.
   */
  private Collection<DssMessage> reportUnreachableBlockEnd() {
    return ImmutableList.of(
        messageFactory.createDssUnreachableBlockEndMessage(block.getId(), status));
  }

  private Collection<DssMessage> reportViolationConditions(
      Collection<ArgPathAndCondition> pRelevantViolations)
      throws InterruptedException, CPAException, SolverException {
    ImmutableListMultimap.Builder<ViolationConditionProgramPoint, AbstractState>
        statePerProgramCounterBuilder = ImmutableListMultimap.builder();
    for (ArgPathAndCondition pathAndCondition : pRelevantViolations) {
      Optional<AbstractState> violationCondition =
          dcpa.getViolationConditionOperator()
              .computeViolationCondition(
                  pathAndCondition.path(), Optional.ofNullable(pathAndCondition.condition()));
      if (violationCondition.isPresent()) {
        statePerProgramCounterBuilder.put(
            new ViolationConditionProgramPoint(
                Optional.ofNullable(pathAndCondition.condition()),
                dcpa.computeProgramPointId(violationCondition.orElseThrow())),
            violationCondition.orElseThrow());
      }
    }
    ImmutableListMultimap<ViolationConditionProgramPoint, AbstractState> statePerProgramCounter =
        statePerProgramCounterBuilder.build();
    Preconditions.checkState(
        !statePerProgramCounter.isEmpty(),
        "The analysis found a feasible counterexample "
            + "which could not be reestablished with the violation-condition operator.");
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
    ImmutableList<String> ids = preconditions.getIdsOfExploredStates();
    return ImmutableSet.of(
        messageFactory.createViolationConditionMessage(
            block.getId(), status, ids, serialize(allVcs)));
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
        DistributedConfigurableProgramAnalysis.MULTIPLE_STATES_KEY,
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
