// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ForwardBlockAnalysis extends BlockAnalysis {

  private final DistributedCompositeCPA distributedCompositeCPA;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final AbstractState top;

  private Precision precision;
  private boolean alreadyReportedError;

  private final boolean containsLoops;

  public ForwardBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      BlockSummaryAnalysisOptions pOptions)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    super(
        pLogger,
        pBlock,
        pCFA,
        AnalysisDirection.FORWARD,
        pSpecification,
        pConfiguration,
        pShutdownManager,
        pOptions);
    containsLoops = pCFA.getAllLoopHeads().isPresent() || pOptions.shouldSendEveryErrorMessage();
    alreadyReportedError = containsLoops;
    reachedSet = getReachedSet();
    precision = getInitialPrecision();
    top = getTop();
    distributedCompositeCPA = getDistributedCompositeCPA();
    block = pBlock;
  }

  @Override
  public Collection<BlockSummaryMessage> analyze(Collection<BlockSummaryMessage> messages)
      throws CPAException, InterruptedException {
    ARGState startState = getStartState(messages);
    Set<ARGState> targetStates = findReachableTargetStatesInBlock(startState);
    if (targetStates.isEmpty()) {
      // if final node is not reachable, do not broadcast anything.
      // in case abstraction is enabled, this might occur since we abstract at block end
      // TODO: Maybe even shutdown workers only listening to this worker??
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    Set<ARGState> violations = extractViolations(targetStates);
    if (!violations.isEmpty() && (!alreadyReportedError || containsLoops)) {
      // we only need to report error locations once
      // since every new report of an already found location would only cause redundant work
      answers.addAll(createErrorConditionMessages(violations));
      alreadyReportedError = true;
    }

    Set<ARGState> blockEntries = extractBlockTargetStates(targetStates);
    answers.addAll(createBlockPostConditionMessage(messages, blockEntries));
    // find all states with location at the end, make formula
    return answers.build();
  }

  @Override
  public Collection<BlockSummaryMessage> performInitialAnalysis()
      throws InterruptedException, CPAException {
    BlockSummaryMessage initial =
        BlockSummaryMessage.newBlockPostCondition(
            block.getId(),
            block.getStartNode().getNodeNumber(),
            BlockSummaryMessagePayload.empty(),
            false,
            true,
            ImmutableSet.of());
    Collection<BlockSummaryMessage> result = analyze(ImmutableSet.of(initial));
    if (reachedSet.getLastState() != null) {
      precision = reachedSet.getPrecision(reachedSet.getLastState());
    }
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
      throws CPAException, InterruptedException {
    List<AbstractState> compositeStates =
        transformedImmutableListCopy(
            blockEntries, state -> AbstractStates.extractStateByType(state, CompositeState.class));
    if (reachedSet.getLastState() != null) {
      precision = reachedSet.getPrecision(reachedSet.getLastState());
    }
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    if (!compositeStates.isEmpty()) {
      boolean fullPath =
          messages.size() == block.getPredecessors().size()
              && messages.stream()
                  .allMatch(m -> ((BlockSummaryPostConditionMessage) m).representsFullPath());
      Set<String> visited = visitedBlocks(messages);
      AbstractState combined =
          Iterables.getOnlyElement(
              distributedCompositeCPA
                  .getCombineOperator()
                  .combine(compositeStates, top, precision));
      BlockSummaryMessagePayload result =
          distributedCompositeCPA.getSerializeOperator().serialize(combined);
      result = appendStatus(getStatus(), result);
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
      answers.add(response);
    }
    return answers.build();
  }

  private Collection<BlockSummaryMessage> createErrorConditionMessages(Set<ARGState> violations)
      throws InterruptedException {
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    for (ARGState targetState : violations) {
      Optional<CFANode> targetNode = abstractStateToLocation(targetState);
      if (targetNode.isEmpty()) {
        throw new AssertionError(
            "States need to have a location but this one does not:" + targetState);
      }
      BlockSummaryMessagePayload initial =
          distributedCompositeCPA
              .getSerializeOperator()
              .serialize(
                  distributedCompositeCPA.getInitialState(
                      targetNode.orElseThrow(), StateSpacePartition.getDefaultPartition()));
      initial = appendStatus(getStatus(), initial);
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
}
