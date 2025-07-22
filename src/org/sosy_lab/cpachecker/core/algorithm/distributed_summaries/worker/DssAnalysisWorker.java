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
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssMessageBroadcaster;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPreconditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage.StatisticsKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics.ThreadCPUTimer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DssAnalysisWorker extends DssWorker {

  private final BlockNode block;

  private final DssBlockAnalysis dssBlockAnalysis;
  private final LogManager logger;
  private final DssMessageFactory messageFactory;
  private boolean shutdown;

  private final DssConnection connection;

  private final ThreadCPUTimer forwardAnalysisTime = new ThreadCPUTimer("Forward Analysis");
  private final ThreadCPUTimer backwardAnalysisTime = new ThreadCPUTimer("Backward Analysis");

  /**
   * {@link DssAnalysisWorker}s trigger forward and backward analyses to find a verification
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
  DssAnalysisWorker(
      String pId,
      DssAnalysisOptions pOptions,
      DssConnection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      DssMessageFactory pMessageFactory,
      ShutdownManager pShutdownManager,
      LogManager pLogger)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("analysis-worker-" + pId, pMessageFactory, pLogger);
    block = pBlock;
    connection = pConnection;

    Configuration forwardConfiguration =
        Configuration.builder()
            .loadFromFile(pOptions.getForwardConfiguration())
            .setOption(
                "alwaysAtGivenLocations",
                Integer.toString(pBlock.getFinalLocation().getNodeNumber()))
            .build();

    messageFactory = pMessageFactory;
    logger = pLogger;
    dssBlockAnalysis =
        new DssBlockAnalysis(
            logger,
            pBlock,
            pCFA,
            pSpecification,
            forwardConfiguration,
            pOptions,
            pMessageFactory,
            pShutdownManager);
  }

  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, SolverException, InterruptedException, InvalidConfigurationException {
    return dssBlockAnalysis.runInitialAnalysis();
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage message) {
    return switch (message.getType()) {
      case VIOLATION_CONDITION -> {
        try {
          backwardAnalysisTime.start();
          yield dssBlockAnalysis.runAnalysisUnderCondition(
              (DssViolationConditionMessage) message, true);
        } catch (Exception | Error e) {
          yield ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e));
        } finally {
          backwardAnalysisTime.stop();
        }
      }
      case PRECONDITION -> {
        try {
          forwardAnalysisTime.start();
          yield dssBlockAnalysis.runAnalysis((DssPreconditionMessage) message);
        } catch (Exception | Error e) {
          yield ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e));
        } finally {
          forwardAnalysisTime.stop();
        }
      }
      case EXCEPTION, RESULT -> {
        shutdown = true;
        yield ImmutableSet.of(messageFactory.createDssStatisticsMessage(getBlockId(), getStats()));
      }
      case STATISTIC -> ImmutableSet.of();
    };
  }

  public void storeMessage(DssMessage message) throws SolverException, InterruptedException {
    switch (message.getType()) {
      case STATISTIC, RESULT, EXCEPTION -> {}
      case VIOLATION_CONDITION -> {
        DssViolationConditionMessage errorCond = (DssViolationConditionMessage) message;
        dssBlockAnalysis.updateViolationCondition(errorCond);
        dssBlockAnalysis.updateSeenPrefixes(errorCond);
      }
      case PRECONDITION -> {
        //noinspection ResultOfMethodCallIgnored
        dssBlockAnalysis.shouldRepeatAnalysis((DssPreconditionMessage) message);
      }
    }
  }

  @Override
  public DssConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  @Override
  public void broadcast(Collection<DssMessage> pMessages) throws InterruptedException {
    DssMessageBroadcaster broadcaster = getConnection().getBroadcaster();
    for (DssMessage message : pMessages) {
      sentMessages.inc();
      switch (message.getType()) {
        case PRECONDITION -> {
          broadcaster.broadcastToObserver(message);
          broadcaster.broadcastToIds(message, block.getSuccessorIds());
        }
        case VIOLATION_CONDITION -> {
          broadcaster.broadcastToObserver(message);
          if (block.getPredecessorIds().isEmpty()) {
            broadcaster.broadcastToAll(
                messageFactory.createDssResultMessage(getId(), Result.FALSE));
          } else {
            broadcaster.broadcastToIds(message, block.getPredecessorIds());
          }
        }
        case EXCEPTION, RESULT, STATISTIC -> broadcaster.broadcastToAll(message);
      }
    }
  }

  @Override
  public void run() {
    try {
      broadcast(dssBlockAnalysis.runInitialAnalysis());
      super.run();
    } catch (Exception | Error e) {
      logger.logException(Level.SEVERE, e, "Worker stopped working due to an error...");
      broadcastOrLogException(
          ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e)));
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

  private ImmutableMap<StatisticsKey, String> getStats() {
    ImmutableMap.Builder<StatisticsKey, String> stats = ImmutableMap.builder();

    if (dssBlockAnalysis.getDCPA() instanceof DistributedARGCPA arg
        && arg.getWrappedCPA() instanceof DistributedCompositeCPA composite) {
      stats.putAll(composite.getStatistics().getStatistics());
    }

    return stats
        .put(
            StatisticsKey.PRECONDITION_CALCULATION_TIME, Long.toString(forwardAnalysisTime.nanos()))
        .put(
            StatisticsKey.VIOLATION_CONDITION_CALCULATION_TIME,
            Long.toString(backwardAnalysisTime.nanos()))
        .put(StatisticsKey.MESSAGES_SENT, Integer.toString(getSentMessages()))
        .put(StatisticsKey.MESSAGES_RECEIVED, Integer.toString(getReceivedMessages()))
        .buildOrThrow();
  }
}
