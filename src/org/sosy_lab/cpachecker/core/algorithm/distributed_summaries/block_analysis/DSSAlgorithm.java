// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DSSAlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DSSUtils.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DSSMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSPostConditionMessage;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class DSSAlgorithm {

  private final DSSPreconditions preconditions;
  private final DSSErrorConditions errorConditions;

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final Algorithm algorithm;
  private final ReachedSet reachedSet;
  private final BlockNode blockNode;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  public DSSAlgorithm(
      BlockNode pBlockNode,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    AnalysisComponents parts =
        DSSAlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlockNode);
    blockNode = pBlockNode;
    algorithm = parts.algorithm();
    reachedSet = parts.reached();
    ConfigurableProgramAnalysis cpa = parts.cpa();
    dcpa =
        DCPAAlgorithmFactory.distribute(
            cpa, pBlockNode, pCFA, pConfiguration, pLogger, pShutdownManager.getNotifier());
    preconditions = new DSSPreconditions(pBlockNode, dcpa);
    errorConditions = new DSSErrorConditions(pBlockNode, dcpa);
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfiguration, pLogger, pCFA.getMachineModel());
  }

  public Collection<DSSMessage> performAnalysis()
      throws CPAException, InterruptedException, SolverException {
    preconditions.prepareReachedSet(reachedSet);
    errorConditions.prepareReachedSet(reachedSet);
    BlockAnalysisIntermediateResult result =
        DSSUtils.findReachableTargetStatesInBlock(algorithm, reachedSet, blockNode);
    Collection<AbstractState> conditions;
    if (errorConditions.getErrorConditions().isEmpty()) {
      // TODO: not nice but works for now
      conditions = new ArrayList<>(1);
      conditions.add(null);
    } else {
      conditions = errorConditions.getErrorConditions();
    }
    return processIntermediateResult(result, conditions);
  }

  public Collection<DSSMessage> processPostConditionMessage(
      DSSPostConditionMessage pBlockSummaryPostConditionMessage)
      throws SolverException, InterruptedException, CPAException {
    DSSMessageProcessing processing =
        preconditions.updatePrecondition(pBlockSummaryPostConditionMessage);
    if (!processing.shouldProceed()) {
      return processing;
    }
    if (preconditions.isFixpointReached()) {
      return ImmutableSet.of();
    }
    return performAnalysis();
  }

  public Collection<DSSMessage> processErrorConditionMessage(
      DSSErrorConditionMessage pErrorConditionMessage)
      throws SolverException, InterruptedException, CPAException {
    DSSMessageProcessing processing = errorConditions.updateErrorCondition(pErrorConditionMessage);
    if (!processing.shouldProceed()) {
      return processing;
    }
    preconditions.prepareReachedSet(reachedSet);
    errorConditions.prepareReachedSet(reachedSet, pErrorConditionMessage.getBlockId());
    BlockAnalysisIntermediateResult result =
        DSSUtils.findReachableTargetStatesInBlock(algorithm, reachedSet, blockNode);
    return processIntermediateResult(
        result,
        ImmutableSet.of(
            errorConditions.getLastErrorConditionOf(pErrorConditionMessage.getBlockId())));
  }

  private Collection<DSSMessage> processIntermediateResult(
      BlockAnalysisIntermediateResult result, Collection<AbstractState> availableErrorConditions)
      throws CPAException, InterruptedException, SolverException {
    if (result.getViolations().isEmpty()) {
      ImmutableSet<@NonNull AbstractState> abstractStates =
          FluentIterable.from(result.getAbstractions()).filter(AbstractState.class).toSet();
      return reportBlockPostConditions(abstractStates, result.getStatus());
    }
    ImmutableSet.Builder<DSSMessage> violationMessages = ImmutableSet.builder();
    for (AbstractState errorCondition : availableErrorConditions) {
      violationMessages.addAll(
          reportErrorConditions(
              result.getViolations(), (ARGState) errorCondition, result.getStatus()));
    }
    return violationMessages.build();
  }

  private AbstractState combine(Collection<AbstractState> states)
      throws InterruptedException, CPAException {
    if (states.isEmpty()) {
      return dcpa.getInitialState(blockNode.getFirst(), StateSpacePartition.getDefaultPartition());
    }
    if (states.size() == 1) {
      return Iterables.getOnlyElement(states);
    }
    List<AbstractState> abstractStates = ImmutableList.copyOf(states);
    AbstractState first = abstractStates.get(0);
    for (int i = 1; i < abstractStates.size(); i++) {
      first = dcpa.getCombineOperator().combine(first, abstractStates.get(i));
    }
    return first;
  }

  private Collection<DSSMessage> reportErrorConditions(
      Set<ARGState> violations, ARGState condition, AlgorithmStatus status)
      throws CPAException, InterruptedException, SolverException {
    ImmutableSet.Builder<DSSMessage> messages = ImmutableSet.builder();
    boolean conditionInitiallyNull = condition == null;
    for (ARGState violation : violations) {
      Optional<CounterexampleInfo> counterexample =
          ARGUtils.tryGetOrCreateCounterexampleInformation(
              violation, dcpa.getCPA(), assumptionToEdgeAllocator);
      if (conditionInitiallyNull) {
        condition =
            (ARGState)
                dcpa.getInitialState(
                    Objects.requireNonNull(AbstractStates.extractLocation(violation)),
                    StateSpacePartition.getDefaultPartition());
      }
      dcpa.computeViolationCondition(
              counterexample
                  .orElseThrow(() -> new AssertionError("Counterexample must be retrievable"))
                  .getTargetPath(),
              condition)
          .ifFeasible(
              state -> {
                DSSMessagePayload violationConditionMessage =
                    dcpa.getSerializeOperator().serialize(state);
                messages.add(
                    DSSMessage.newErrorConditionMessage(
                        blockNode.getId(),
                        blockNode.getFirst().getNodeNumber(),
                        DSSUtils.appendStatus(status, violationConditionMessage),
                        false,
                        blockNode.getId()));
              });
    }
    return messages.build();
  }

  private Collection<DSSMessage> reportBlockPostConditions(
      Set<AbstractState> postConditions, AlgorithmStatus status)
      throws CPAException, InterruptedException {
    ImmutableSet.Builder<DSSMessage> messages = ImmutableSet.builder();
    DSSMessagePayload postConditionMessage =
        dcpa.getSerializeOperator().serialize(combine(postConditions));
    messages.add(
        DSSMessage.newBlockPostCondition(
            blockNode.getId(),
            blockNode.getLast().getNodeNumber(),
            DSSUtils.appendStatus(status, postConditionMessage),
            true));
    return messages.build();
  }

  public DistributedConfigurableProgramAnalysis getDcpa() {
    return dcpa;
  }
}
