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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.AlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class DCPABackwardAlgorithm {

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Precision initialPrecision;
  private final Algorithm algorithm;
  private final LogManager logger;
  private final DCPAAlgorithm forwardAnalysis;

  public DCPABackwardAlgorithm(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      DCPAAlgorithm pForwardAnalysis,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    AnalysisComponents parts =
        AlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    algorithm = parts.algorithm();
    ConfigurableProgramAnalysis cpa = parts.cpa();
    reachedSet = parts.reached();
    status = AlgorithmStatus.SOUND_AND_PRECISE;

    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    initialPrecision = reachedSet.getPrecision(Objects.requireNonNull(reachedSet.getFirstState()));

    block = pBlock;
    dcpa =
        DistributedConfigurableProgramAnalysis.distribute(cpa, pBlock, AnalysisDirection.BACKWARD);
    logger = pLogger;
    forwardAnalysis = pForwardAnalysis;
  }

  private AlgorithmStatus status;

  public Collection<BlockSummaryMessage> runAnalysisForMessage(
      BlockSummaryErrorConditionMessage pReceived)
      throws SolverException, InterruptedException, CPAException {
    AbstractState deserialized =
        new ARGState(dcpa.getDeserializeOperator().deserialize(pReceived), null);
    BlockSummaryMessageProcessing processing = dcpa.getProceedOperator().proceed(deserialized);
    if (processing.end()) {
      return processing;
    }
    assert processing.isEmpty() : "Proceed is not possible with unprocessed messages";

    Collection<BlockSummaryMessage> messages = ImmutableSet.of();
    if (!pReceived.isFirst()) {
      messages =
          forwardAnalysis.runAnalysisUnderCondition(Optional.of(translateToForward(deserialized)));

      if (FluentIterable.from(messages)
          .filter(BlockSummaryPostConditionMessage.class)
          .filter(BlockSummaryPostConditionMessage::isReachable)
          .isEmpty()) {
        return ImmutableSet.<BlockSummaryMessage>builder()
            .addAll(messages)
            .add(
                BlockSummaryMessage.newErrorConditionUnreachableMessage(
                    block.getId(), "forward analysis failed"))
            .build();
      }
    }
    reachedSet.clear();
    reachedSet.add(deserialized, initialPrecision);
    BlockAnalysisIntermediateResult result =
        DCPAAlgorithms.findReachableTargetStatesInBlock(
            algorithm, reachedSet, block.getStartNode());
    Set<ARGState> targetStates = result.getBlockEnds();
    status = status.update(result.getStatus());
    List<AbstractState> states =
        transformedImmutableListCopy(
            targetStates, state -> AbstractStates.extractStateByType(state, CompositeState.class));
    if (states.isEmpty()) {
      // should only happen if abstraction is activated
      logger.log(Level.ALL, "Cannot reach block start?", reachedSet);
      return ImmutableSet.<BlockSummaryMessage>builder()
          .addAll(messages)
          .add(
              BlockSummaryMessage.newErrorConditionUnreachableMessage(
                  block.getId(), "backwards analysis cannot reach target at block entry"))
          .build();
    }
    ImmutableSet.Builder<BlockSummaryMessage> responses = ImmutableSet.builder();
    for (AbstractState state : states) {
      BlockSummaryMessagePayload payload = dcpa.getSerializeOperator().serialize(state);
      ImmutableSet<String> visited =
          ImmutableSet.<String>builder().addAll(ImmutableSet.of()).add(block.getId()).build();
      payload = DCPAAlgorithms.appendStatus(status, payload);
      responses.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(), block.getStartNode().getNodeNumber(), payload, false, visited));
    }
    return responses.addAll(messages).build();
  }

  private AbstractState translateToForward(AbstractState pBackwardAnalysis)
      throws InterruptedException {
    BlockSummaryMessage forwardMessage =
        BlockSummaryMessage.newBlockPostCondition(
            block.getId(),
            block.getAbstractionNode().getNodeNumber(),
            getDCPA().getSerializeOperator().serialize(pBackwardAnalysis),
            false,
            true,
            ImmutableSet.of());
    return forwardAnalysis.getDCPA().getDeserializeOperator().deserialize(forwardMessage);
  }

  public DistributedConfigurableProgramAnalysis getDCPA() {
    return dcpa;
  }
}
