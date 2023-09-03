// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPAFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class InferDCPAAlgorithm {

  private final String functionName;
  private final DistributedConfigurableProgramAnalysis dcpa;
  private final BlockNode block;
  private Precision blockStartPrecision;
  private final AbstractState startState;
  private final Algorithm algorithm;
  private final ReachedSet reachedSet;
  protected AlgorithmStatus status;

  // json field which identifies which funciton is sending a message
  public static final String MESSAGE_FUNCTION = "messageFunction";
  public static final String RUN_ORDER = "runOrder";
  public int runCounter;

  public InferDCPAAlgorithm(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    functionName = pBlock.getFirst().getFunctionName();
    block = pBlock;
    status = AlgorithmStatus.SOUND_AND_PRECISE;

    AnalysisComponents parts =
        DCPAAlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);

    algorithm = parts.algorithm();
    reachedSet = parts.reached();
    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    reachedSet.clear();
    dcpa =
        DCPAFactory.distribute(
            parts.cpa(), pBlock, AnalysisDirection.FORWARD, pCFA, pConfiguration);

    blockStartPrecision =
        dcpa.getInitialPrecision(block.getFirst(), StateSpacePartition.getDefaultPartition());
    startState = dcpa.getInitialState(block.getFirst(), StateSpacePartition.getDefaultPartition());
    runCounter = 0;
  }

  public Collection<BlockSummaryMessage> runAnalysisUnderCondition(
      Optional<AbstractState> errorCondition, MessageType pMessageType, String pMessageFunction)
      throws CPAException, InterruptedException {
    reachedSet.clear();
    reachedSet.add(startState, blockStartPrecision);

    // set error condition to all starting states if present
    errorCondition.ifPresent(
        condition ->
            reachedSet.forEach(
                abstractState -> {
                  BlockState blockState =
                      Objects.requireNonNull(
                          AbstractStates.extractStateByType(abstractState, BlockState.class));
                  blockState.addErrorCondition(pMessageFunction, errorCondition.orElseThrow());
                  blockState.setStrengthenType(pMessageFunction, pMessageType);
                }));

    BlockAnalysisIntermediateResult result =
        DCPAAlgorithms.findReachableTargetStatesInBlock(
            algorithm, reachedSet, block, AnalysisDirection.FORWARD);
    return processIntermediateResult(result);
  }

  private BlockSummaryMessage createPostConditionMessage(
      BlockSummaryMessagePayload pPayload, int runCount) {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(MESSAGE_FUNCTION, functionName)
            .addEntry(RUN_ORDER, runCount)
            .buildPayload();
    return BlockSummaryMessage.newBlockPostCondition(
        block.getId(),
        block.getLast().getNodeNumber(),
        DCPAAlgorithms.appendStatus(status, payload),
        true);
  }

  private Collection<BlockSummaryMessage> createErrorConditionMessages(Set<ARGState> violations) {
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    int runCount = runCounter++;
    for (ARGState targetState : violations) {
      Optional<CFANode> targetNode = DCPAAlgorithms.abstractStateToLocation(targetState);
      if (targetNode.isEmpty()) {
        throw new AssertionError(
            "States need to have a location but this one does not: " + targetState);
      }
      BlockSummaryMessagePayload initial = dcpa.getSerializeOperator().serialize(targetState);
      BlockSummaryMessagePayload withName =
          BlockSummaryMessagePayload.builder()
              .addAllEntries(initial)
              .addEntry(MESSAGE_FUNCTION, functionName)
              .addEntry(RUN_ORDER, runCount)
              .buildPayload();
      BlockSummaryMessagePayload withStatus = DCPAAlgorithms.appendStatus(status, withName);
      answers.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(), targetNode.orElseThrow().getNodeNumber(), withStatus, true));
    }
    return answers.build();
  }

  private Collection<BlockSummaryMessage> reportUnreachableBlockEnd() {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(MESSAGE_FUNCTION, functionName)
            .addEntry(RUN_ORDER, runCounter++)
            .buildPayload();

    return ImmutableSet.of(
        BlockSummaryMessage.newBlockPostCondition(
            block.getId(),
            block.getLast().getNodeNumber(),
            DCPAAlgorithms.appendStatus(AlgorithmStatus.SOUND_AND_PRECISE, payload),
            false));
  }

  public Collection<BlockSummaryMessage> processIntermediateResult(
      BlockAnalysisIntermediateResult result) {
    status = status.update(result.getStatus());
    assert reachedSet.getFirstState() != null;
    // no feasible path to block end ==> block end is unreachable and successors
    // can remove this block from their predecessor set.
    if (result.isEmpty()) {
      return reportUnreachableBlockEnd();
    }

    if (!result.getViolations().isEmpty()) {
      return createErrorConditionMessages(result.getViolations());
    }

    // empty block ends imply that there was no abstraction node reached
    assert !result.getBlockEnds().isEmpty() || result.getBlockTargets().isEmpty();
    if (!result.getBlockEnds().isEmpty()) {
      int runCount = runCounter++;
      return FluentIterable.from(result.getBlockEnds())
          .transform(state -> dcpa.serialize(state, reachedSet.getPrecision(state)))
          .transform(p -> createPostConditionMessage(p, runCount))
          .toList();
    } else {
      return reportUnreachableBlockEnd();
    }
  }

  public Collection<BlockSummaryMessage> runInitialAnalysis()
      throws CPAException, InterruptedException {
    reachedSet.clear();
    reachedSet.add(startState, blockStartPrecision);
    Collection<BlockSummaryMessage> results =
        processIntermediateResult(
            DCPAAlgorithms.findReachableTargetStatesInBlock(
                algorithm, reachedSet, block, AnalysisDirection.FORWARD));
    if (results.isEmpty()) {
      return reportUnreachableBlockEnd();
    }
    return results;
  }

  public DistributedConfigurableProgramAnalysis getDCPA() {
    return dcpa;
  }
}
