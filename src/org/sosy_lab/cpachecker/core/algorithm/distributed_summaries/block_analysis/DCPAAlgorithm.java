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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.AlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class DCPAAlgorithm {

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final Map<String, BlockSummaryMessage> states;
  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private Precision initialPrecision;
  private Precision lastPrecision;
  private final Algorithm algorithm;
  private final AbstractState startState;
  private final Set<String> predecessors;

  private AlgorithmStatus status;
  private boolean alreadyReportedError;
  private boolean alreadyReportedInfeasibility;

  public DCPAAlgorithm(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    alreadyReportedError = false;
    alreadyReportedInfeasibility = false;
    AnalysisComponents parts =
        AlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    algorithm = parts.algorithm();
    cpa = parts.cpa();
    reachedSet = parts.reached();

    status = AlgorithmStatus.SOUND_AND_PRECISE;

    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    initialPrecision = reachedSet.getPrecision(Objects.requireNonNull(reachedSet.getFirstState()));
    lastPrecision = initialPrecision;
    startState = reachedSet.getFirstState();

    states = new HashMap<>();

    block = pBlock;
    dcpa =
        DistributedConfigurableProgramAnalysis.distribute(
            pConfiguration, pLogger, cpa, pBlock, AnalysisDirection.FORWARD);
    predecessors = transformedImmutableSetCopy(block.getPredecessors(), BlockNodeMetaData::getId);
  }

  public Collection<BlockSummaryMessage> reportUnreachableBlockEnd() {
    // if sent once, it will never change (precondition is always the most general information)
    if (alreadyReportedInfeasibility) {
      return ImmutableSet.of();
    }
    alreadyReportedInfeasibility = true;
    return ImmutableSet.of(
        BlockSummaryMessage.newBlockPostCondition(
            block.getId(),
            block.getLastNode().getNodeNumber(),
            DCPAAlgorithms.appendStatus(
                AlgorithmStatus.SOUND_AND_PRECISE, BlockSummaryMessagePayload.empty()),
            // we can assume full here as no precondition will ever change unsatisfiability of
            // this block
            true,
            false,
            ImmutableSet.of()));
  }

  public Collection<BlockSummaryMessage> runInitialAnalysis()
      throws CPAException, InterruptedException {
    reachedSet.clear();
    reachedSet.add(startState, initialPrecision);
    Collection<BlockSummaryMessage> results =
        processIntermediateResult(
            DCPAAlgorithms.findReachableTargetStatesInBlock(
                algorithm, reachedSet, block.getLastNode()));
    if (results.isEmpty()) {
      return reportUnreachableBlockEnd();
    }
    return results;
  }

  public Collection<BlockSummaryMessage> runAnalysisForMessage(
      BlockSummaryPostConditionMessage pReceived)
      throws SolverException, InterruptedException, CPAException {
    AbstractState deserialized =
        new ARGState(dcpa.getDeserializeOperator().deserialize(pReceived), null);
    initialPrecision = dcpa.getDeserializePrecisionOperator().deserializePrecision(pReceived);
    if (predecessors.contains(pReceived.getBlockId())) {
      if (pReceived.isReachable()) {
        states.put(pReceived.getBlockId(), pReceived);
      } else {
        states.put(pReceived.getBlockId(), null);
      }
    }
    BlockSummaryMessageProcessing processing = dcpa.getProceedOperator().proceed(deserialized);
    if (processing.end()) {
      if (predecessors.contains(pReceived.getBlockId())) {
        states.put(pReceived.getBlockId(), null);
      }
      return processing;
    }
    assert processing.isEmpty() : "Proceed is not possible with unprocessed messages";
    BlockSummaryMessage previousMessage = states.get(pReceived.getUniqueBlockId());
    AbstractState previous = null;
    if (previousMessage != null) {
      AbstractState abstractState = dcpa.getDeserializeOperator().deserialize(previousMessage);
      if (abstractState != null) {
        previous = new ARGState(abstractState, null);
      }
    }
    if (states.containsKey(pReceived.getUniqueBlockId())) {
      if (previous != null && cpa.getAbstractDomain().isLessOrEqual(previous, deserialized)) {
        return BlockSummaryMessageProcessing.stop();
      }
    }
    states.put(pReceived.getBlockId(), pReceived);
    if (states.size() != block.getPredecessors().size()) {
      return ImmutableSet.of();
    }
    return runAnalysisUnderCondition(Optional.empty());
  }

  public Collection<BlockSummaryMessage> runAnalysisUnderCondition(
      Optional<AbstractState> errorCondition) throws CPAException, InterruptedException {
    reachedSet.clear();
    for (BlockSummaryMessage message : states.values()) {
      if (message == null) {
        continue;
      }
      AbstractState value = new ARGState(dcpa.getDeserializeOperator().deserialize(message), null);
      if (reachedSet.isEmpty()) {
        reachedSet.add(value, initialPrecision);
      } else {
        for (AbstractState abstractState : reachedSet) {
          AbstractState merged =
              cpa.getMergeOperator().merge(value, abstractState, initialPrecision);
          if (!merged.equals(abstractState)) {
            reachedSet.remove(value);
          }
          reachedSet.add(merged, initialPrecision);
        }
      }
    }

    if (reachedSet.isEmpty()) {
      reachedSet.add(
          cpa.getInitialState(block.getStartNode(), StateSpacePartition.getDefaultPartition()),
          initialPrecision);
    }

    for (AbstractState abstractState : reachedSet) {
      // safe since distributed analyses cannot work without block cpa
      Objects.requireNonNull(AbstractStates.extractStateByType(abstractState, BlockState.class))
          .setErrorCondition(errorCondition);
    }

    BlockAnalysisIntermediateResult result =
        DCPAAlgorithms.findReachableTargetStatesInBlock(algorithm, reachedSet, block.getLastNode());
    return processIntermediateResult(result);
  }

  private Collection<BlockSummaryMessage> processIntermediateResult(
      BlockAnalysisIntermediateResult result) throws InterruptedException {
    status = status.update(result.getStatus());
    if (reachedSet.getLastState() != null) {
      initialPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
      lastPrecision = reachedSet.getPrecision(reachedSet.getLastState());
    }
    if (result.isEmpty()) {
      return reportUnreachableBlockEnd();
    }
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    // empty block ends imply that there was no abstraction node reached
    assert !result.getBlockEnds().isEmpty() || result.getBlockTargets().isEmpty();
    if (!result.getBlockEnds().isEmpty()) {
      answers.addAll(
          FluentIterable.from(result.getBlockEnds())
              .transform(
                  state -> DCPAAlgorithms.chainSerialization(dcpa).apply(state, lastPrecision))
              .transform(
                  p ->
                      BlockSummaryMessage.newBlockPostCondition(
                          block.getId(),
                          block.getLastNode().getNodeNumber(),
                          p,
                          false,
                          true,
                          ImmutableSet.of()))
              .toSet());
    } else {
      answers.addAll(reportUnreachableBlockEnd());
    }
    if (!result.getViolations().isEmpty() && !alreadyReportedError) {
      alreadyReportedError = true;
      return createErrorConditionMessages(result.getViolations());
    }
    return answers.build();
  }

  private Collection<BlockSummaryMessage> createErrorConditionMessages(Set<ARGState> violations)
      throws InterruptedException {
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    for (ARGState targetState : violations) {
      Optional<CFANode> targetNode = DCPAAlgorithms.abstractStateToLocation(targetState);
      if (targetNode.isEmpty()) {
        throw new AssertionError(
            "States need to have a location but this one does not: " + targetState);
      }
      BlockSummaryMessagePayload initial =
          dcpa.getSerializeOperator()
              .serialize(
                  dcpa.getInitialState(
                      targetNode.orElseThrow(), StateSpacePartition.getDefaultPartition()));
      initial = DCPAAlgorithms.appendStatus(status, initial);
      answers.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(),
              targetNode.orElseThrow().getNodeNumber(),
              initial,
              true,
              ImmutableSet.of(block.getId())));
    }
    return answers.build();
  }

  public DistributedConfigurableProgramAnalysis getDCPA() {
    return dcpa;
  }
}
