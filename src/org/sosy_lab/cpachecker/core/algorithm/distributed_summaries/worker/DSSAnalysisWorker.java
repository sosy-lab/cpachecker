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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DSSAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DSSStatistics.ThreadCPUTimer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSStatisticsMessage.BlockSummaryStatisticType;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DSSAnalysisWorker extends DSSWorker {

  private final BlockNode block;

  private final DSSAlgorithm dcpaAlgorithm;
  private final LogManager logger;
  private boolean shutdown;

  private final DSSConnection connection;

  private final ThreadCPUTimer forwardAnalysisTime = new ThreadCPUTimer("Forward Analysis");
  private final ThreadCPUTimer backwardAnalysisTime = new ThreadCPUTimer("Backward Analysis");

  /**
   * {@link DSSAnalysisWorker}s trigger forward and backward analyses to find a verification
   * verdict.
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
  DSSAnalysisWorker(
      String pId,
      DSSOptions pOptions,
      DSSConnection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager,
      LogManager pLogger)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("analysis-worker-" + pId, pLogger);
    block = pBlock;
    connection = pConnection;

    Configuration forwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getForwardConfiguration()).build();

    logger = pLogger;
    dcpaAlgorithm =
        new DSSAlgorithm(
            pBlock, logger, pCFA, pSpecification, forwardConfiguration, pShutdownManager);
  }

  @Override
  public Collection<DSSMessage> processMessage(DSSMessage message) {
    switch (message.getType()) {
      case ERROR_CONDITION -> {
        try {
          backwardAnalysisTime.start();
          return dcpaAlgorithm.processErrorConditionMessage((DSSErrorConditionMessage) message);
        } catch (Exception | Error e) {
          return ImmutableSet.of(DSSMessage.newErrorMessage(getBlockId(), e));
        } finally {
          backwardAnalysisTime.stop();
        }
      }
      case BLOCK_POSTCONDITION -> {
        try {
          forwardAnalysisTime.start();
          return dcpaAlgorithm.processPostConditionMessage((DSSPostConditionMessage) message);
        } catch (Exception | Error e) {
          return ImmutableSet.of(DSSMessage.newErrorMessage(getBlockId(), e));
        } finally {
          forwardAnalysisTime.stop();
        }
      }
      case ERROR, FOUND_RESULT -> {
        shutdown = true;
        return ImmutableSet.of(DSSMessage.newStatisticsMessage(getBlockId(), getStats()));
      }
      case ERROR_CONDITION_UNREACHABLE, STATISTICS -> {
        return ImmutableSet.of();
      }
      default -> throw new AssertionError("MessageType " + message.getType() + " does not exist");
    }
  }

  public void storeMessage(DSSMessage message)
      throws SolverException, InterruptedException, CPAException {
    switch (message.getType()) {
      case STATISTICS, FOUND_RESULT, ERROR, ERROR_CONDITION_UNREACHABLE -> {}
      case ERROR_CONDITION -> {
        //noinspection ResultOfMethodCallIgnored
        dcpaAlgorithm.updateErrorCondition((DSSErrorConditionMessage) message);
      }
      case BLOCK_POSTCONDITION -> {
        //noinspection ResultOfMethodCallIgnored
        dcpaAlgorithm.updatePrecondition((DSSPostConditionMessage) message);
      }
      default -> throw new AssertionError("MessageType " + message.getType() + " does not exist");
    }
  }

  public Collection<DSSMessage> performInitialAnalysis()
      throws CPAException, SolverException, InterruptedException {
    return dcpaAlgorithm.performAnalysis();
  }

  @Override
  public DSSConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  @Override
  public void run() {
    try {
      broadcast(dcpaAlgorithm.performAnalysis());
      super.run();
    } catch (Exception | Error e) {
      logger.logException(Level.SEVERE, e, "Worker stopped working due to an error...");
      broadcastOrLogException(ImmutableSet.of(DSSMessage.newErrorMessage(getBlockId(), e)));
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
    if (dcpaAlgorithm.getDcpa() instanceof DistributedARGCPA arg) {
      if (arg.getWrappedCPA() instanceof DistributedCompositeCPA composite) {
        forwardDCPA = composite;
      }
    }

    return ImmutableMap.<String, Object>builder()
        .put(BlockSummaryStatisticType.FORWARD_TIME.name(), forwardAnalysisTime.nanos())
        .put(BlockSummaryStatisticType.BACKWARD_TIME.name(), backwardAnalysisTime.nanos())
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
