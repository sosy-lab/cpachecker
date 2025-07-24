// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.java_smt.api.SolverException;

public class DssBlockAnalysis {

  private record AnalysisComponents(
      Algorithm algorithm, ConfigurableProgramAnalysis cpa, ReachedSet reached) {}

  private final DistributedConfigurableProgramAnalysis dcpa;
  // In this map, value 'null' means that the key 'blockId' is a loop predecessor
  // from which we have not seen any summary message yet
  private final Map<String, DssPreconditionMessage> preconditions;
  private final Map<String, DssViolationConditionMessage> violationConditions;
  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final LogManager logger;

  private final DssMessageFactory messageFactory;

  // forward analysis variables
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
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfiguration, pLogger, pCFA.getMachineModel());
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

    preconditions = new LinkedHashMap<>();
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
      Set<ARGState> blockEnds, boolean allowTop) {
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    ARGState abstraction = Iterables.getOnlyElement(blockEnds);
    if (dcpa.isTop(abstraction) && !allowTop) {
      return messages.build();
    }
    ImmutableMap<String, String> serialized =
        dcpa.serialize(abstraction, reachedSet.getPrecision(abstraction));
    messages.add(
        messageFactory.createDssPreconditionMessage(block.getId(), true, status, serialized));
    return messages.build();
  }

  private Collection<DssMessage> reportViolationConditions(
      Set<ARGState> violations, ARGState condition, boolean first)
      throws CPAException, InterruptedException, SolverException {
    ImmutableSet.Builder<ARGPath> pathsToViolations = ImmutableSet.builder();
    pathsToViolations.addAll(
        transformedImmutableSetCopy(
            violations,
            v ->
                ARGUtils.tryGetOrCreateCounterexampleInformation(
                        v, dcpa.getCPA(), assumptionToEdgeAllocator)
                    .orElseThrow()
                    .getTargetPath()));
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    for (ARGPath path : pathsToViolations.build()) {
      Optional<AbstractState> violationCondition =
          dcpa.getViolationConditionOperator()
              .computeViolationCondition(path, Optional.ofNullable(condition));
      if (violationCondition.isEmpty()) {
        continue;
      }
      ImmutableMap<String, String> content =
          dcpa.serialize(
              violationCondition.orElseThrow(), reachedSet.getPrecision(path.getLastState()));
      messages.add(
          messageFactory.createViolationConditionMessage(block.getId(), status, first, content));
    }
    return messages.build();
  }

  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException, InvalidConfigurationException {
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

    return reportViolationConditions(result.getViolations(), null, true);
  }

  /**
   * Adds a new abstract state to the known start states and execute the configured forward
   * analysis.
   *
   * @param pReceived Current message to process
   * @return All violations and/or abstractions that occurred while running the forward analysis.
   */
  public Collection<DssMessage> runAnalysis(DssPreconditionMessage pReceived)
      throws SolverException, InterruptedException, CPAException, InvalidConfigurationException {
    logger.log(Level.INFO, "Running forward analysis with new precondition");
    AbstractState deserialized = dcpa.getDeserializeOperator().deserialize(pReceived);
    DssMessageProcessing processing = dcpa.getProceedOperator().processForward(deserialized);
    if (!processing.shouldProceed()) {
      return processing;
    }
    if (preconditions.containsKey(pReceived.getSenderId())) {
      AbstractState previous =
          dcpa.getDeserializeOperator().deserialize(preconditions.get(pReceived.getSenderId()));
      if (dcpa.getCoverageOperator().covers(previous, deserialized)) {
        // we already have a precondition implying by the new one
        return ImmutableSet.of();
      }
    }
    preconditions.put(pReceived.getSenderId(), pReceived);
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    for (DssViolationConditionMessage violation : violationConditions.values()) {
      messages.addAll(runAnalysisUnderCondition(violation));
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
   * Runs the CPA under an error condition, i.e., if the current block contains a block-end edge,
   * the error condition will be attached to that edge. In case this makes the path formula
   * infeasible, we compute an abstraction. If no error condition is present, we run the CPA.
   *
   * @param pViolationCondition a message containing an abstract state representing an error
   *     condition
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   */
  public Collection<DssMessage> runAnalysisUnderCondition(
      DssViolationConditionMessage pViolationCondition)
      throws CPAException, InterruptedException, SolverException, InvalidConfigurationException {
    logger.log(Level.INFO, "Running forward analysis with respect to error condition");
    violationConditions.put(pViolationCondition.getSenderId(), pViolationCondition);
    // merge all states into the reached set
    AbstractState violationCondition =
        dcpa.getDeserializeOperator().deserialize(pViolationCondition);
    DssMessageProcessing processing = dcpa.getProceedOperator().processBackward(violationCondition);
    if (!processing.shouldProceed()) {
      return processing;
    }

    prepareReachedSet();

    reachedSet.forEach(
        abstractState ->
            Objects.requireNonNull(
                    AbstractStates.extractStateByType(abstractState, BlockState.class))
                .setViolationCondition(violationCondition));

    DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    if (!result.getSummaries().isEmpty() && result.getViolations().isEmpty()) {
      messages.addAll(reportBlockPostConditions(result.getSummaries(), false));
    }
    if (!result.getViolations().isEmpty()) {
      messages.addAll(
          reportViolationConditions(
              result.getViolations(), ((ARGState) violationCondition), false));
    }
    return messages.build();
  }

  /**
   * Prepare the reached set for next analysis by merging all received BPC messages into a non-empty
   * set of start states.
   *
   * @throws CPAException thrown in merge or stop operation runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly.
   */
  private void prepareReachedSet() throws CPAException, InterruptedException {
    // simulate merge and stop for all states ending up at block#getStartNode
    reachedSet.clear();
    ImmutableList.Builder<StateAndPrecision> deserializedStates = ImmutableList.builder();
    for (Entry<String, DssPreconditionMessage> messageEntry : preconditions.entrySet()) {
      DssMessage message = messageEntry.getValue();
      StateAndPrecision stateAndPrecision = dcpa.deserialize(message);
      if (dcpa.isTop(stateAndPrecision.state())
          && block.getLoopPredecessorIds().contains(messageEntry.getKey())) {
        continue;
      }
      deserializedStates.add(stateAndPrecision);
    }
    DssBlockAnalyses.executeCpaAlgorithmWithStates(reachedSet, cpa, deserializedStates.build());
    if (reachedSet.isEmpty()) {
      reachedSet.add(makeStartState(), makeStartPrecision());
    }
  }

  public DistributedConfigurableProgramAnalysis getDCPA() {
    return dcpa;
  }
}
