// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPAFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** The same as {@link DCPAAlgorithm} but adds called functions to ErrorConditionMessage payloads */
public class InferDCPAAlgorithm extends DCPAAlgorithm {

  public static final String CALLED_FUNCTIONS = "calledFunctions";

  private final Set<String> callees;
  private final BlockNode block;
  private AlgorithmStatus status;
  private final ConfigurableProgramAnalysis cpa;
  private final DistributedConfigurableProgramAnalysis dcpa;

  public InferDCPAAlgorithm(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(pLogger, pBlock, pCFA, pSpecification, pConfiguration, pShutdownManager);
    block = pBlock;
    status = AlgorithmStatus.SOUND_AND_PRECISE;
    AnalysisComponents parts =
        DCPAAlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    cpa = parts.cpa();
    dcpa = DCPAFactory.distribute(cpa, pBlock, AnalysisDirection.FORWARD, pCFA, pConfiguration);
    callees = getCalledFunctions();
  }

  @Override
  protected Collection<BlockSummaryMessage> createErrorConditionMessages(Set<ARGState> violations)
      throws InterruptedException {
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    for (ARGState targetState : violations) {
      Optional<CFANode> targetNode = DCPAAlgorithms.abstractStateToLocation(targetState);
      if (targetNode.isEmpty()) {
        throw new AssertionError(
            "States need to have a location but this one does not: " + targetState);
      }
      BlockSummaryMessagePayload initial = dcpa.getSerializeOperator().serialize(targetState);
      initial =
          BlockSummaryMessagePayload.builder()
              .addAllEntries(initial)
              .addEntry(CALLED_FUNCTIONS, callees)
              .buildPayload();
      initial = DCPAAlgorithms.appendStatus(status, initial);
      answers.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(), targetNode.orElseThrow().getNodeNumber(), initial, true));
    }
    return answers.build();
  }

  @Override
  protected BlockSummaryMessage createPostConditionMessage(BlockSummaryMessagePayload pPayload) {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(CALLED_FUNCTIONS, callees)
            .buildPayload();
    return BlockSummaryMessage.newBlockPostCondition(
        block.getId(),
        block.getLast().getNodeNumber(),
        DCPAAlgorithms.appendStatus(status, payload),
        true);
  }

  private ImmutableSet<String> getCalledFunctions() {
    return FluentIterable.from(block.getEdges())
        .filter(FunctionSummaryEdge.class)
        .transform(edge -> edge.getFunctionEntry().getFunctionName())
        .toSet();
  }
}
