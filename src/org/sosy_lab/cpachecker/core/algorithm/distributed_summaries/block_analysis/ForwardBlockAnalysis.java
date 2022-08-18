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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.BlockAnalysisUtil.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.java_smt.api.SolverException;

public class ForwardBlockAnalysis implements InitialBlockAnalyzer, ContinuousBlockAnalyzer {

  private final DistributedCompositeCPA distributedCompositeCPA;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final AbstractState top;
  private final Algorithm algorithm;
  private AlgorithmStatus status;

  private Precision precision;
  private boolean alreadyReportedError;
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
  public ForwardBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      BlockSummaryAnalysisOptions pOptions)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    alreadyReportedError = false;
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> parts =
        AlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    algorithm = parts.getFirst();
    ConfigurableProgramAnalysis cpa = Objects.requireNonNull(parts.getSecond());
    reachedSet = parts.getThird();

    status = AlgorithmStatus.SOUND_AND_PRECISE;

    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    precision = reachedSet.getPrecision(Objects.requireNonNull(reachedSet.getFirstState()));

    top = cpa.getInitialState(pBlock.getStartNode(), StateSpacePartition.getDefaultPartition());
    block = pBlock;
    distributedCompositeCPA =
        (DistributedCompositeCPA)
            DistributedConfigurableProgramAnalysis.distribute(
                cpa, pBlock, AnalysisDirection.FORWARD, pOptions);
  }

  @Override
  public Collection<BlockSummaryMessage> analyze(Collection<BlockSummaryMessage> messages)
      throws CPAException, InterruptedException, SolverException {
    ARGState startState =
        BlockAnalysisUtil.getStartState(
            block.getStartNode(), precision, distributedCompositeCPA, messages);
    reachedSet.clear();
    reachedSet.add(startState, precision);
    BlockAnalysisIntermediateResult result =
        BlockAnalysisUtil.findReachableTargetStatesInBlock(algorithm, reachedSet);
    status = status.update(result.getStatus());
    // update precision for start state
    precision = reachedSet.getPrecision(startState);
    if (result.isEmpty()) {
      // if final node is not reachable, do not broadcast anything.
      // in case abstraction is enabled, this might occur since we abstract at block end
      // TODO: Maybe even shutdown workers only listening to this worker??
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    Set<ARGState> violations = result.getTargets();
    if (!violations.isEmpty() && !alreadyReportedError) {
      // we only need to report error locations once
      // since every new report of an already found location would only cause redundant work
      answers.addAll(createErrorConditionMessages(violations));
      alreadyReportedError = true;
    }

    Set<ARGState> blockEntries = result.getBlockTargets();
    answers.addAll(createBlockPostConditionMessage(messages, blockEntries));
    // find all states with location at the end, make formula
    return answers.build();
  }

  @Override
  public Collection<BlockSummaryMessage> performInitialAnalysis()
      throws InterruptedException, CPAException, SolverException {
    BlockSummaryMessage initial =
        BlockSummaryMessage.newBlockPostCondition(
            block.getId(),
            block.getStartNode().getNodeNumber(),
            BlockSummaryMessagePayload.empty(),
            false,
            true,
            ImmutableSet.of());
    Collection<BlockSummaryMessage> result = analyze(ImmutableSet.of(initial));
    if (result.isEmpty()) {
      // full path = true as no predecessor can ever change unreachability of block exit
      return ImmutableSet.of(
          BlockSummaryMessage.newBlockPostCondition(
              block.getId(),
              block.getStartNode().getNodeNumber(),
              BlockSummaryMessagePayload.empty(),
              true,
              false,
              ImmutableSet.of()));
    }
    return result;
  }

  private Collection<BlockSummaryMessage> createBlockPostConditionMessage(
      Collection<BlockSummaryMessage> messages, Set<ARGState> blockEntries)
      throws InterruptedException, CPAException {
    ImmutableList<AbstractState> compositeStates =
        transformedImmutableListCopy(
            blockEntries, state -> AbstractStates.extractStateByType(state, CompositeState.class));
    if (!compositeStates.isEmpty()) {
      boolean fullPath =
          messages.size() == block.getPredecessors().size()
              && messages.stream()
                  .allMatch(m -> ((BlockSummaryPostConditionMessage) m).representsFullPath());
      ImmutableSet<String> visited =
          ImmutableSet.<String>builder()
              .addAll(BlockAnalysisUtil.findVisitedBlocks(messages))
              .add(block.getId())
              .build();
      AbstractState combined =
          Iterables.getOnlyElement(
              distributedCompositeCPA
                  .getCombineOperator()
                  .combine(compositeStates, top, precision));
      BlockSummaryMessagePayload result =
          distributedCompositeCPA.getSerializeOperator().serialize(combined);
      result = BlockAnalysisUtil.appendStatus(status, result);
      BlockSummaryPostConditionMessage response =
          (BlockSummaryPostConditionMessage)
              BlockSummaryMessage.newBlockPostCondition(
                  block.getId(),
                  block.getLastNode().getNodeNumber(),
                  result,
                  fullPath,
                  true,
                  visited);
      distributedCompositeCPA.getProceedOperator().update(response);
      return ImmutableSet.of(response);
    }
    return ImmutableSet.of();
  }

  private Collection<BlockSummaryMessage> createErrorConditionMessages(Set<ARGState> violations)
      throws InterruptedException {
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    for (ARGState targetState : violations) {
      Optional<CFANode> targetNode = BlockAnalysisUtil.abstractStateToLocation(targetState);
      if (targetNode.isEmpty()) {
        throw new AssertionError(
            "States need to have a location but this one does not: " + targetState);
      }
      BlockSummaryMessagePayload initial =
          distributedCompositeCPA
              .getSerializeOperator()
              .serialize(
                  distributedCompositeCPA.getInitialState(
                      targetNode.orElseThrow(), StateSpacePartition.getDefaultPartition()));
      initial = BlockAnalysisUtil.appendStatus(status, initial);
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

  public DistributedCompositeCPA getDistributedCPA() {
    return distributedCompositeCPA;
  }
}
