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
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.java_smt.api.SolverException;

public class DssBlockAnalysis {

  private record AnalysisComponents(
      Algorithm algorithm, ConfigurableProgramAnalysis cpa, ReachedSet reached) {}

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final DssMessageFactory messageFactory;
  private final Multimap<String, @NonNull StateAndPrecision> preconditions;
  private final Map<String, @NonNull StateAndPrecision> violationConditions;

  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Algorithm algorithm;

  private final LogManager logger;

  private AlgorithmStatus status;

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
    violationConditions = new LinkedHashMap<>();
  }

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

  public Collection<DssMessage> reportUnreachableBlockEnd() {
    return ImmutableSet.of(
        messageFactory.createDssPreconditionMessage(
            block.getId(), false, status, ImmutableMap.of()));
  }

  private Collection<DssMessage> reportBlockPostConditions(
      Set<@NonNull ARGState> blockEnds, boolean allowTop) {
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    for (ARGState abstraction : blockEnds) {
      if (dcpa.isMostGeneralBlockEntryState(abstraction) && !allowTop) {
        return messages.build();
      }
    }
    ImmutableMap<String, String> serialized =
        dcpa.serialize(
            transformedImmutableListCopy(
                blockEnds, a -> new StateAndPrecision(a, reachedSet.getPrecision(a))));
    messages.add(
        messageFactory.createDssPreconditionMessage(block.getId(), true, status, serialized));
    return messages.build();
  }

  private Collection<DssMessage> reportFirstViolationConditions(Set<@NonNull ARGState> violations)
      throws CPAException, InterruptedException, SolverException {
    return reportViolationConditions(violations, null, true);
  }

  record ArgPathWithEdges(List<ARGState> states, List<CFAEdge> edges) {

    private ARGState getLastState() {
      return states.get(states.size() - 1);
    }

    private ArgPathWithEdges copyWith(ARGState pNewParent, List<CFAEdge> pEdges) {
      if (!edges.isEmpty()) {
        CFAEdge lastEdge = edges.get(edges.size() - 1);
        if (!lastEdge.getPredecessor().equals(pEdges.get(0).getSuccessor())) {
          List<CFAEdge> path = new ArrayList<>();
          path.add(lastEdge);
          CFAEdge last = lastEdge;
          while (!last.getSuccessor().equals(pEdges.get(0).getPredecessor())) {
            Collection<CFAEdge> successors = CFAUtils.enteringEdges(last.getPredecessor()).toList();
            path.add(Iterables.getOnlyElement(successors));
            last = path.get(path.size() - 1);
          }
          pEdges = ImmutableList.<CFAEdge>builder().addAll(pEdges).addAll(path).build();
        }
      }
      return new ArgPathWithEdges(
          ImmutableList.<ARGState>builder().addAll(states).add(pNewParent).build(),
          ImmutableList.<CFAEdge>builder().addAll(edges).addAll(pEdges).build());
    }
  }

  private Collection<ARGPath> allArgPathsFromState(ARGState state) {
    List<ArgPathWithEdges> waitlist = new ArrayList<>();
    waitlist.add(new ArgPathWithEdges(ImmutableList.of(state), ImmutableList.of()));
    ImmutableList.Builder<ARGPath> finished = ImmutableList.builder();
    while (!waitlist.isEmpty()) {
      ArgPathWithEdges current = waitlist.remove(waitlist.size() - 1);
      ARGState last = current.getLastState();
      if (last.getParents().isEmpty()) {
        finished.add(
            new ARGPath(
                Lists.reverse(current.states()),
                Lists.reverse(current.edges()),
                Lists.reverse(current.edges())));
        continue;
      }
      for (ARGState parent : last.getParents()) {
        waitlist.add(current.copyWith(parent, Lists.reverse(parent.getEdgesToChild(last))));
      }
    }
    return finished.build();
  }

  private Collection<ARGPath> collectAllArgPaths(Set<@NonNull ARGState> states) {
    ImmutableList.Builder<ARGPath> builder = ImmutableList.builder();
    for (ARGState state : states) {
      builder.addAll(allArgPathsFromState(state));
    }
    return builder.build();
  }

  private Collection<DssMessage> reportViolationConditions(
      Set<@NonNull ARGState> violations, ARGState condition, boolean first)
      throws CPAException, InterruptedException, SolverException {
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    for (ARGPath path : collectAllArgPaths(violations)) {
      Optional<AbstractState> violationCondition =
          dcpa.getViolationConditionOperator()
              .computeViolationCondition(path, Optional.ofNullable(condition));
      if (violationCondition.isEmpty()) {
        continue;
      }
      ImmutableMap<String, String> content =
          dcpa.serialize(
              ImmutableList.of(
                  new StateAndPrecision(
                      violationCondition.orElseThrow(),
                      reachedSet.getPrecision(path.getLastState()))));
      messages.add(
          messageFactory.createViolationConditionMessage(block.getId(), status, first, content));
    }
    return messages.build();
  }

  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    reachedSet.clear();
    reachedSet.add(makeStartState(), makeStartPrecision());

    DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    if (result.getViolations().isEmpty()) {
      if (result.getSummaries().isEmpty()) {
        return reportUnreachableBlockEnd();
      }
      return reportBlockPostConditions(result.getSummaries(), true);
    }

    return reportFirstViolationConditions(result.getViolations());
  }

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
    ImmutableSet.Builder<StateAndPrecision> discard = ImmutableSet.builder();
    if (preconditions.containsKey(pReceived.getSenderId())) {
      // whether a fixpoint was reached
      Collection<StateAndPrecision> previousStates = preconditions.get(pReceived.getSenderId());
      int covered = 0;
      for (StateAndPrecision deserialized : deserializedStates) {
        for (StateAndPrecision previous : previousStates) {
          if (dcpa.isMostGeneralBlockEntryState(previous.state())) {
            discard.add(previous);
            continue;
          }
          if (dcpa.getCoverageOperator().isSubsumed(previous.state(), deserialized.state())) {
            covered++;
            discard.add(deserialized);
            break;
          }
          if (dcpa.getCoverageOperator().isSubsumed(deserialized.state(), previous.state())) {
            discard.add(previous);
          }
        }
      }
      if (covered == deserializedStates.size()) {
        // we already have a precondition implying the new one
        return DssMessageProcessing.stop();
      }
    }
    if (!block.getLoopPredecessorIds().contains(pReceived.getSenderId())) {
      discard.addAll(preconditions.get(pReceived.getSenderId()));
    }
    ImmutableSet<StateAndPrecision> discarded = discard.build();
    preconditions.putAll(pReceived.getSenderId(), deserializedStates);
    discarded.forEach(sp -> preconditions.remove(pReceived.getSenderId(), sp));
    return processing;
  }

  public DssMessageProcessing storeViolationCondition(
      DssViolationConditionMessage pNewViolationCondition)
      throws InterruptedException, SolverException {
    logger.log(Level.INFO, "Running forward analysis with respect to error condition");
    // merge all states into the reached set
    StateAndPrecision violationCondition =
        Iterables.getOnlyElement(dcpa.deserialize(pNewViolationCondition));
    DssMessageProcessing processing =
        dcpa.getProceedOperator().processBackward(violationCondition.state());
    if (!processing.shouldProceed()) {
      return processing;
    }
    violationConditions.put(pNewViolationCondition.getSenderId(), violationCondition);
    return processing;
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
    for (StateAndPrecision stateAndPrecision : violationConditions.values()) {
      messages.addAll(analyzeViolationCondition(stateAndPrecision));
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
    StateAndPrecision violation = violationConditions.get(pSenderId);
    if (violation == null) {
      throw new IllegalArgumentException(
          "No violation condition found for sender ID: " + pSenderId);
    }
    return analyzeViolationCondition(violation);
  }

  /**
   * Runs the CPA under an error condition, i.e., if the current block contains a block-end edge,
   * the error condition will be attached to that edge. In case this makes the path formula
   * infeasible, we compute an abstraction. If no error condition is present, we run the CPA.
   *
   * @param violation The violation condition to analyze, which is a precise summary of all
   *     specification violations
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   */
  public Collection<DssMessage> analyzeViolationCondition(StateAndPrecision violation)
      throws CPAException, InterruptedException, SolverException {
    prepareReachedSet();
    // debugPrecondition();

    reachedSet.forEach(
        abstractState ->
            Objects.requireNonNull(
                    AbstractStates.extractStateByType(abstractState, BlockState.class))
                .setViolationCondition(violation.state()));

    DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    if (!result.getSummaries().isEmpty()) {
      messages.addAll(reportBlockPostConditions(result.getSummaries(), false));
    }
    if (!result.getViolations().isEmpty()) {
      messages.addAll(
          reportViolationConditions(result.getViolations(), ((ARGState) violation.state()), false));
    }
    return messages.build();
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
  private void prepareReachedSet() throws CPAException, InterruptedException {
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
      if (block.getLoopPredecessorIds().contains(predecessorId)) {
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
  }

  public DistributedConfigurableProgramAnalysis getDcpa() {
    return dcpa;
  }
}
