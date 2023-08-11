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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPAFactory;
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
  private final boolean defaultRootWorker;
  public Set<List<String>> collectedBlockSummaryErrorMessages = new HashSet<>();

  BlockSummaryRootWorker(
      String pId,
      boolean Infer,
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
    defaultRootWorker = !Infer;
    shutdown = false;
    root = pNode;
    AnalysisComponents parts =
        DCPAAlgorithmFactory.createAlgorithm(
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
        BlockSummaryMessageProcessing processing =
            dcpa.getProceedOperator().proceed(currentState);
        if (!defaultRootWorker) {
          collectedBlockSummaryErrorMessages.add(
              ((BlockSummaryErrorConditionMessage) pMessage).getViolations());
          //getLogger().log(Level.INFO,collectedBlockSummaryErrorMessages);
        }
        yield ImmutableSet.of(
            BlockSummaryMessage.newResultMessage(
                root.getId(), root.getLast().getNodeNumber(), Result.FALSE));
      }
      case FOUND_RESULT, EXCEPTION -> {
        shutdown = true;
        yield ImmutableSet.of();
      }
      case STATISTICS, ERROR_CONDITION_UNREACHABLE, BLOCK_POSTCONDITION -> ImmutableSet.of();
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
    } catch (InterruptedException e) {
      getLogger().logException(Level.SEVERE, e, "Root worker stopped unexpectedly.");
      broadcastOrLogException(ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getId(), e)));
    }
  }
}
