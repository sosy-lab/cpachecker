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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage.StatisticsKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics.ThreadCPUTimer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.java_smt.api.SolverException;

public class DssAnalysisWorker extends DssWorker implements AutoCloseable {

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
                "cpa.predicate.blk.alwaysAtGivenNodes",
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
      throws CPAException, SolverException, InterruptedException {
    return dssBlockAnalysis.runInitialAnalysis();
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage message) {
    return switch (message.getType()) {
      case POST_CONDITION -> {
        try {
          forwardAnalysisTime.start();
          DssMessageProcessing processing =
              dssBlockAnalysis.storePrecondition((DssPostConditionMessage) message);
          if (!processing.shouldProceed()) {
            yield processing;
          }
          yield dssBlockAnalysis.analyzePrecondition(message.getSenderId());
        } catch (Exception | Error e) {
          yield ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e));
        } finally {
          forwardAnalysisTime.stop();
        }
      }
      case VIOLATION_CONDITION -> {
        try {
          backwardAnalysisTime.start();
          DssMessageProcessing processing =
              dssBlockAnalysis.storeViolationCondition((DssViolationConditionMessage) message);
          if (!processing.shouldProceed()) {
            yield processing;
          }
          yield dssBlockAnalysis.analyzeViolationCondition(message.getSenderId());
        } catch (Exception | Error e) {
          yield ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e));
        } finally {
          backwardAnalysisTime.stop();
        }
      }
      case EXCEPTION, RESULT -> {
        shutdown = true;
        yield ImmutableSet.of(messageFactory.createDssStatisticsMessage(getBlockId(), getStats()));
      }
      case STATISTIC -> ImmutableSet.of();
    };
  }

  @CanIgnoreReturnValue
  public DssMessageProcessing storeMessage(DssMessage message)
      throws SolverException, InterruptedException, CPAException {
    return switch (message.getType()) {
      case STATISTIC, RESULT, EXCEPTION -> DssMessageProcessing.stop();
      case VIOLATION_CONDITION ->
          dssBlockAnalysis.storeViolationCondition((DssViolationConditionMessage) message);
      case POST_CONDITION -> dssBlockAnalysis.storePrecondition((DssPostConditionMessage) message);
    };
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
        case POST_CONDITION -> {
          broadcaster.broadcastToObserver(message);
          broadcaster.broadcastToIds(
              message, ImmutableSet.copyOf(((DssPostConditionMessage) message).getReceivers()));
        }
        case VIOLATION_CONDITION -> {
          if (block.getPredecessorIds().isEmpty()) {
            broadcaster.broadcastToAll(
                messageFactory.createDssResultMessage(getId(), Result.FALSE));
          } else {
            broadcaster.broadcastToObserver(message);
            broadcaster.broadcastToIds(message, block.getPredecessorIds());
          }
        }
        case EXCEPTION, RESULT, STATISTIC -> broadcaster.broadcastToAll(message);
      }
    }
  }

  public void broadcastInitialMessages()
      throws CPAException, SolverException, InterruptedException {
    broadcast(dssBlockAnalysis.runInitialAnalysis());
  }

  @Override
  public void run() {
    try {
      broadcastInitialMessages();
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

    if (dssBlockAnalysis.getDcpa() instanceof DistributedARGCPA arg
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

  @Override
  public void close() {
    logger.log(Level.INFO, "Worker finished and shuts down.");
    CPAs.closeCpaIfPossible(dssBlockAnalysis.getDcpa(), logger);
  }
}
