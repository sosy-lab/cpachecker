// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryStatisticsMessage.BlockSummaryStatisticType;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.java_smt.api.SolverException;

public class BlockSummaryAnalysisWorker extends BlockSummaryWorker {

  private final BlockNode block;

  private final DCPAAlgorithm dcpaAlgorithm;
  private boolean shutdown;

  private final BlockSummaryConnection connection;

  private final StatTimer forwardAnalysisTime = new StatTimer("Forward Analysis");
  private final StatTimer backwardAnalysisTime = new StatTimer("Backward Analysis");

  /**
   * {@link BlockSummaryAnalysisWorker}s trigger forward and backward analyses to find a
   * verification verdict.
   *
   * @param pId unique id of worker that will be prefixed with 'analysis-worker-'
   * @param pOptions analysis options for distributed analysis
   * @param pConnection unique connection to other actors
   * @param pBlock block where this analysis works on
   * @param pCFA complete CFA of which pBlock is a subgraph
   * @param pSpecification specification that should not be violated
   * @param pShutdownManager handler for unexpected shutdowns
   * @throws CPAException exceptions that are logged
   * @throws InterruptedException thrown if user exits program
   * @throws InvalidConfigurationException thrown if configuration contains unexpected values
   * @throws IOException thrown if socket and/or files are not readable
   */
  BlockSummaryAnalysisWorker(
      String pId,
      BlockSummaryAnalysisOptions pOptions,
      BlockSummaryConnection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("analysis-worker-" + pId, pOptions);
    block = pBlock;
    connection = pConnection;

    Configuration forwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getForwardConfiguration()).build();

    dcpaAlgorithm =
        new DCPAAlgorithm(
            getLogger(), pBlock, pCFA, pSpecification, forwardConfiguration, pShutdownManager);
  }

  public Collection<BlockSummaryMessage> runInitialAnalysis()
      throws CPAException, SolverException, InterruptedException {
    return dcpaAlgorithm.runInitialAnalysis();
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage message) {
    switch (message.getType()) {
      case ERROR_CONDITION -> {
        try {
          backwardAnalysisTime.start();
          return dcpaAlgorithm.runAnalysisUnderCondition(
              (BlockSummaryErrorConditionMessage) message);
        } catch (Exception | Error e) {
          return ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getBlockId(), e));
        } finally {
          backwardAnalysisTime.stop();
        }
      }
      case BLOCK_POSTCONDITION -> {
        try {
          forwardAnalysisTime.start();
          return dcpaAlgorithm.runAnalysis((BlockSummaryPostConditionMessage) message);
        } catch (Exception | Error e) {
          return ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getBlockId(), e));
        } finally {
          forwardAnalysisTime.stop();
        }
      }
      case ERROR, FOUND_RESULT -> {
        shutdown = true;
        return ImmutableSet.of(BlockSummaryMessage.newStatisticsMessage(getBlockId(), getStats()));
      }
      case ERROR_CONDITION_UNREACHABLE, STATISTICS -> {
        return ImmutableSet.of();
      }
      default -> throw new AssertionError("MessageType " + message.getType() + " does not exist");
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
      broadcast(dcpaAlgorithm.runInitialAnalysis());
      super.run();
    } catch (Exception | Error e) {
      getLogger().logException(Level.SEVERE, e, "Worker stopped working due to an error...");
      broadcastOrLogException(
          ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getBlockId(), e)));
      shutdown = true;
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  @Override
  public String toString() {
    return "Worker{block=" + block + ", finished=" + shutdownRequested() + '}';
  }

  private Map<String, Object> getStats() {
    DistributedCompositeCPA forwardDCPA = null;
    if (dcpaAlgorithm.getDCPA() instanceof DistributedCompositeCPA) {
      forwardDCPA = (DistributedCompositeCPA) dcpaAlgorithm.getDCPA();
    }
    return ImmutableMap.<String, Object>builder()
        .put(
            BlockSummaryStatisticType.FORWARD_TIME.name(),
            forwardAnalysisTime.getConsumedTime().asNanos())
        .put(
            BlockSummaryStatisticType.BACKWARD_TIME.name(),
            backwardAnalysisTime.getConsumedTime().asNanos())
        .put(BlockSummaryStatisticType.MESSAGES_SENT.name(), Integer.toString(getSentMessages()))
        .put(
            BlockSummaryStatisticType.MESSAGES_RECEIVED.name(),
            Integer.toString(getReceivedMessages()))
        .put(
            BlockSummaryStatisticType.FORWARD_ANALYSIS_STATS.name(),
            forwardDCPA == null ? ImmutableMap.of() : forwardDCPA.getStatistics().getStatistics())
        .buildOrThrow();
  }
}
