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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class BackwardBlockAnalysis extends BlockAnalysis {

  private final ReachedSet reachedSet;
  private final BlockNode block;
  private final LogManager logger;
  private final DistributedCompositeCPA distributedCompositeCPA;

  public BackwardBlockAnalysis(
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
        AnalysisDirection.BACKWARD,
        pSpecification,
        pConfiguration,
        pShutdownManager,
        pOptions);
    reachedSet = getReachedSet();
    block = pBlock;
    logger = pLogger;
    distributedCompositeCPA = getDistributedCompositeCPA();
  }

  @Override
  public Collection<BlockSummaryMessage> analyze(Collection<BlockSummaryMessage> messages)
      throws CPAException, InterruptedException, SolverException {
    ARGState startState = getStartState(messages);
    Set<ARGState> targetStates = findReachableTargetStatesInBlock(startState);
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
      payload = appendStatus(getStatus(), payload);
      responses.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(),
              block.getStartNode().getNodeNumber(),
              payload,
              false,
              visitedBlocks(messages)));
    }
    return responses.build();
  }

  @Override
  public Collection<BlockSummaryMessage> performInitialAnalysis()
      throws InterruptedException, CPAException {
    // current approach does not need an initial backward analysis.
    throw new AssertionError("Initial backward analysis is not implemented yet.");
  }
}
