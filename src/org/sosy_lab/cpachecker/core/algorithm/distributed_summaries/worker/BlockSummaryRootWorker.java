// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.AlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class BlockSummaryRootWorker extends BlockSummaryWorker {

  private final BlockNode root;
  private final BlockSummaryConnection connection;
  private final DistributedConfigurableProgramAnalysis dcpa;
  private final AbstractState topState;
  private boolean shutdown;

  BlockSummaryRootWorker(
      String pId,
      BlockSummaryConnection pConnection,
      BlockSummaryAnalysisOptions pOptions,
      BlockNode pNode,
      CFA pCfa,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("root-worker-" + pId, pOptions);
    checkArgument(
        pNode.isRoot() && pNode.isEmpty() && pNode.getLastNode().equals(pNode.getStartNode()),
        "Root node must be empty and cannot have predecessors: " + "%s",
        pNode);
    Configuration backwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getBackwardConfiguration()).build();

    Specification backwardSpecification =
        Specification.fromFiles(
            ImmutableSet.of(
                Path.of("config/specification/MainEntry.spc"),
                Path.of("config/specification/TerminatingFunctions.spc")),
            pCfa,
            backwardConfiguration,
            getLogger(),
            pShutdownManager.getNotifier());
    connection = pConnection;
    shutdown = false;
    root = pNode;
    AnalysisComponents parts =
        AlgorithmFactory.createAlgorithm(
            getLogger(),
            backwardSpecification,
            pCfa,
            backwardConfiguration,
            pShutdownManager,
            pNode);
    // never needs precision
    dcpa =
        DistributedConfigurableProgramAnalysis.distribute(
            Configuration.builder().build(),
            LogManager.createNullLogManager(),
            parts.cpa(),
            pNode,
            AnalysisDirection.FORWARD);
    topState =
        Objects.requireNonNull(parts.cpa())
            .getInitialState(root.getLastNode(), StateSpacePartition.getDefaultPartition());
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException, CPAException, IOException {
    switch (pMessage.getType()) {
      case ERROR_CONDITION:
        AbstractState currentState = dcpa.getDeserializeOperator().deserialize(pMessage);
        BlockSummaryMessageProcessing processing = dcpa.getProceedOperator().proceed(currentState);
        if (processing.end()) {
          return processing;
        }
        return ImmutableSet.of(
            BlockSummaryMessage.newResultMessage(
                root.getId(),
                root.getLastNode().getNodeNumber(),
                Result.FALSE,
                ((BlockSummaryErrorConditionMessage) pMessage).visitedBlockIds()));
      case FOUND_RESULT:
        // fall through
      case ERROR:
        shutdown = true;
        return ImmutableSet.of();
      case STATISTICS:
        // fall through
      case BLOCK_POSTCONDITION:
        // fall through
      case ERROR_CONDITION_UNREACHABLE:
        return ImmutableSet.of();
      default:
        throw new AssertionError("Unknown MessageType " + pMessage.getType());
    }
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  @Override
  public void run() {
    try {
      broadcast(
          ImmutableSet.of(
              BlockSummaryMessage.newBlockPostCondition(
                  root.getId(),
                  root.getLastNode().getNodeNumber(),
                  dcpa.getSerializeOperator().serialize(topState),
                  true,
                  true,
                  ImmutableSet.of(root.getId()))));
      super.run();
    } catch (InterruptedException pE) {
      getLogger().logException(Level.SEVERE, pE, "Root worker stopped unexpectedly.");
      broadcastOrLogException(ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getId(), pE)));
    }
  }
}
