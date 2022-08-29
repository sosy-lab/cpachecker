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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.BlockAnalysisUtil.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.java_smt.api.SolverException;

public class BackwardBlockAnalysis implements ContinuousBlockAnalyzer<DistributedCompositeCPA> {

  private final ReachedSet reachedSet;
  private final BlockNode block;
  private final DistributedCompositeCPA distributedCompositeCPA;
  private final Algorithm algorithm;
  private final LogManager logger;
  private final Precision precision;
  private AlgorithmStatus status;

  /**
   * Analyzes a subgraph of the CFA (block node) with an arbitrary CPA.
   *
   * @param pLogger logger to log information
   * @param pBlock coherent subgraph of the CFA
   * @param pCFA CFA where the subgraph pBlock is built from
   * @param pSpecification the specification that the analysis should prove correct/wrong
   * @param pConfiguration user defined configurations
   * @param pShutdownManager shutdown manager for unexpected shutdown requests
   * @param pOptions user defined options for block analyses
   * @throws CPAException if the misbehaviour should be logged instead of causing a crash
   * @throws InterruptedException if the analysis is interrupted by the user
   * @throws InvalidConfigurationException if the configurations contain wrong values
   */
  public BackwardBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      BlockSummaryAnalysisOptions pOptions)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    logger = pLogger;
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> parts =
        AlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    algorithm = parts.getFirst();
    ConfigurableProgramAnalysis cpa = parts.getSecond();
    reachedSet = parts.getThird();

    status = AlgorithmStatus.SOUND_AND_PRECISE;

    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    precision = reachedSet.getPrecision(Objects.requireNonNull(reachedSet.getFirstState()));

    block = pBlock;
    distributedCompositeCPA =
        (DistributedCompositeCPA)
            DistributedConfigurableProgramAnalysis.distribute(
                cpa, pBlock, AnalysisDirection.BACKWARD, pOptions);
  }

  @Override
  public Collection<BlockSummaryMessage> analyze(Collection<BlockSummaryMessage> messages)
      throws CPAException, InterruptedException, SolverException {
    ARGState startState =
        BlockAnalysisUtil.getStartState(
            block.getLastNode(), precision, distributedCompositeCPA, messages);
    reachedSet.clear();
    reachedSet.add(startState, precision);
    BlockAnalysisIntermediateResult result =
        BlockAnalysisUtil.findReachableTargetStatesInBlock(algorithm, reachedSet);
    Set<ARGState> targetStates = result.getBlockTargets();
    status = status.update(result.getStatus());
    List<AbstractState> states =
        transformedImmutableListCopy(
            targetStates, state -> AbstractStates.extractStateByType(state, CompositeState.class));
    if (states.isEmpty()) {
      // should only happen if abstraction is activated
      logger.log(Level.ALL, "Cannot reach block start?", reachedSet);
      return ImmutableSet.of(
          BlockSummaryMessage.newErrorConditionUnreachableMessage(
              block.getId(), "backwards analysis cannot reach target at block entry"));
    }
    ImmutableSet.Builder<BlockSummaryMessage> responses = ImmutableSet.builder();
    for (AbstractState state : states) {
      BlockSummaryMessagePayload payload =
          distributedCompositeCPA.getSerializeOperator().serialize(state);
      ImmutableSet<String> visited =
          ImmutableSet.<String>builder()
              .addAll(BlockAnalysisUtil.findVisitedBlocks(messages))
              .add(block.getId())
              .build();
      payload = BlockAnalysisUtil.appendStatus(status, payload);
      responses.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(), block.getStartNode().getNodeNumber(), payload, false, visited));
    }
    return responses.build();
  }

  @Override
  public DistributedCompositeCPA getAnalysis() {
    return distributedCompositeCPA;
  }
}
