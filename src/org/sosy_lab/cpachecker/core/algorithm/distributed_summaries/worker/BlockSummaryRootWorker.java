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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.AlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPAFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
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
        pNode.isRoot() && pNode.isEmpty() && pNode.getLast().equals(pNode.getFirst()),
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
    dcpa = DCPAFactory.distribute(parts.cpa(), pNode, AnalysisDirection.FORWARD, pCfa);
    topState =
        Objects.requireNonNull(parts.cpa())
            .getInitialState(root.getLast(), StateSpacePartition.getDefaultPartition());
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException {
    return switch (pMessage.getType()) {
      case ERROR_CONDITION -> {
        AbstractState currentState = dcpa.getDeserializeOperator().deserialize(pMessage);
        BlockSummaryMessageProcessing processing = dcpa.getProceedOperator().proceed(currentState);
        if (processing.end()) {
          yield processing;
        }
        yield ImmutableSet.of(
            BlockSummaryMessage.newResultMessage(
                root.getId(), root.getLast().getNodeNumber(), Result.FALSE));
      }
      case FOUND_RESULT, ERROR -> {
        shutdown = true;
        yield ImmutableSet.of();
      }
      case STATISTICS, BLOCK_POSTCONDITION, ERROR_CONDITION_UNREACHABLE -> ImmutableSet.of();
    };
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
                  root.getFirst().getNodeNumber(),
                  dcpa.getSerializeOperator().serialize(topState),
                  true)));
      super.run();
    } catch (InterruptedException pE) {
      getLogger().logException(Level.SEVERE, pE, "Root worker stopped unexpectedly.");
      broadcastOrLogException(ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getId(), pE)));
    }
  }
}
