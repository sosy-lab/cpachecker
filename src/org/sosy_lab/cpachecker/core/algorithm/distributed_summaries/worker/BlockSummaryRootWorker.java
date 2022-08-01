// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.BlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.NoopBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ActorMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockPostConditionActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ErrorConditionActorMessage;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class BlockSummaryRootWorker extends BlockSummaryWorker {

  private final BlockNode root;
  private final BlockAnalysis analysis;
  private final Connection connection;
  private boolean shutdown;

  BlockSummaryRootWorker(
      String pId,
      Connection pConnection,
      AnalysisOptions pOptions,
      LogManager pLogManager,
      BlockNode pNode,
      CFA pCfa,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    super("root-worker-" + pId, pLogManager);
    connection = pConnection;
    shutdown = false;
    root = pNode;
    if (!root.isRoot() || !root.isEmpty() || !root.getLastNode().equals(root.getStartNode())) {
      throw new AssertionError("Root node must be empty and cannot have predecessors: " + pNode);
    }
    analysis =
        new NoopBlockAnalysis(
            getLogger(),
            pNode,
            pCfa,
            AnalysisDirection.FORWARD,
            pSpecification,
            pConfiguration,
            pShutdownManager,
            pOptions);
  }

  @Override
  public Collection<ActorMessage> processMessage(ActorMessage pMessage)
      throws InterruptedException, SolverException, CPAException, IOException {
    switch (pMessage.getType()) {
      case ERROR_CONDITION:
        if (pMessage.getTargetNodeNumber() == root.getLastNode().getNodeNumber()
            && root.getSuccessors().stream()
                .anyMatch(block -> block.getId().equals(pMessage.getUniqueBlockId()))) {
          ActorMessageProcessing processing =
              analysis
                  .getDistributedCPA()
                  .getProceedOperator()
                  .proceedBackward((ErrorConditionActorMessage) pMessage);
          if (processing.end()) {
            return processing;
          }
          return ImmutableSet.of(
              ActorMessage.newResultMessage(
                  root.getId(),
                  root.getLastNode().getNodeNumber(),
                  Result.FALSE,
                  ((ErrorConditionActorMessage) pMessage).visitedBlockIds()));
        }
        return ImmutableSet.of();
      case FOUND_RESULT:
        // fall through
      case ERROR:
        shutdown = true;
        return ImmutableSet.of();
      case BLOCK_POSTCONDITION:
        // fall through
      case ERROR_CONDITION_UNREACHABLE:
        return ImmutableSet.of();
      default:
        throw new AssertionError("Unknown MessageType " + pMessage.getType());
    }
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  @Override
  public void run() {
    try {
      Collection<ActorMessage> initialMessage = analysis.performInitialAnalysis();
      analysis
          .getDistributedCPA()
          .getProceedOperator()
          .update((BlockPostConditionActorMessage) initialMessage.stream().findAny().orElseThrow());
      broadcast(initialMessage);
      super.run();
    } catch (InterruptedException | CPAException pE) {
      getLogger().logException(Level.SEVERE, pE, "Root worker stopped unexpectedly.");
      broadcastOrLogException(ImmutableSet.of(ActorMessage.newErrorMessage(getId(), pE)));
    }
  }
}
