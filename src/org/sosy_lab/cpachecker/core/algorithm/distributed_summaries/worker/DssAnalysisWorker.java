// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.base.Preconditions;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssBlockWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssMessageBroadcaster;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.java_smt.api.SolverException;

public class DssAnalysisWorker extends DssWorker implements AutoCloseable {

  @FunctionalInterface
  private interface AnalysisCreation {

    DssBlockAnalysis createDssBlockAnalysis()
        throws CPAException, InvalidConfigurationException, InterruptedException;
  }

  private static class CreateOrRetrieveThreadLocalAnalysis {

    private final AnalysisCreation createAnalysis;
    private DssBlockAnalysis dssBlockAnalysis;
    private String originalThreadName;

    private CreateOrRetrieveThreadLocalAnalysis(AnalysisCreation pAnalysisCreation) {
      createAnalysis = pAnalysisCreation;
    }

    DssBlockAnalysis getDssBlockAnalysis() {
      if (dssBlockAnalysis == null) {
        try {
          dssBlockAnalysis = createAnalysis.createDssBlockAnalysis();
        } catch (InterruptedException | InvalidConfigurationException | CPAException e) {
          throw new AssertionError("Could not create DssBlockAnalysis but it is required", e);
        }
        originalThreadName = Thread.currentThread().getName();
      }
      assert originalThreadName != null && dssBlockAnalysis != null;
      Preconditions.checkState(
          wouldBeCalledFromCorrectThread(), "Cannot invoke analysis from different thread.");
      return dssBlockAnalysis;
    }

    boolean wouldBeCalledFromCorrectThread() {
      return Thread.currentThread().getName().equals(originalThreadName);
    }
  }

  private final CreateOrRetrieveThreadLocalAnalysis analysis;

  private final BlockNode block;

  private final LogManager logger;
  private final DssMessageFactory messageFactory;

  private final DssConnection connection;

  private final DssBlockWorkerStatistics workerStats;

  private boolean shutdown;
  private boolean closed;

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
      DssWorkerStatistics pWorkerStatistics,
      LogManager pLogger)
      throws InvalidConfigurationException, IOException {
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
    workerStats = pWorkerStatistics.createWorkerStats(pId);
    analysis =
        new CreateOrRetrieveThreadLocalAnalysis(
            () ->
                new DssBlockAnalysis(
                    logger,
                    pBlock,
                    pCFA,
                    pSpecification,
                    forwardConfiguration,
                    pOptions,
                    pMessageFactory,
                    pShutdownManager,
                    workerStats));
  }

  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, SolverException, InterruptedException {
    return analysis.getDssBlockAnalysis().runInitialAnalysis();
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage message) {
    workerStats.getMessagesReceived().inc();
    return switch (message.getType()) {
      case POST_CONDITION -> {
        try {
          DssMessageProcessing processing =
              analysis.getDssBlockAnalysis().storePrecondition((DssPostConditionMessage) message);
          if (!processing.shouldProceed()) {
            yield processing;
          }
          yield analysis.getDssBlockAnalysis().analyzePrecondition();
        } catch (Exception | Error e) {
          yield ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e));
        }
      }
      case VIOLATION_CONDITION -> {
        try {
          DssMessageProcessing processing =
              analysis
                  .getDssBlockAnalysis()
                  .storeViolationCondition((DssViolationConditionMessage) message);
          if (!processing.shouldProceed()) {
            yield processing;
          }
          yield analysis.getDssBlockAnalysis().analyzeViolationCondition(message.getSenderId());
        } catch (Exception | Error e) {
          yield ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e));
        }
      }
      case EXCEPTION, RESULT -> {
        shutdown = true;
        close();
        yield ImmutableSet.of();
      }
    };
  }

  @CanIgnoreReturnValue
  public DssMessageProcessing storeMessage(DssMessage message)
      throws SolverException, InterruptedException, CPAException {
    return switch (message.getType()) {
      case RESULT, EXCEPTION -> DssMessageProcessing.stop();
      case VIOLATION_CONDITION ->
          analysis
              .getDssBlockAnalysis()
              .storeViolationCondition((DssViolationConditionMessage) message);
      case POST_CONDITION ->
          analysis.getDssBlockAnalysis().storePrecondition((DssPostConditionMessage) message);
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
      workerStats.getMessagesSent().inc();
      switch (message.getType()) {
        case POST_CONDITION -> {
          broadcaster.broadcastToObserver(message);
          broadcaster.broadcastToIds(message, block.getSuccessorIds());
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
        case EXCEPTION, RESULT -> {
          broadcaster.broadcastToAll(message);
          close();
        }
      }
    }
  }

  public void broadcastInitialMessages()
      throws CPAException, SolverException, InterruptedException {
    broadcast(analysis.getDssBlockAnalysis().runInitialAnalysis());
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
      close();
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

  @Override
  public void close() {
    logger.logf(Level.INFO, "Worker %s called close().", getId());
    if (!closed && analysis.wouldBeCalledFromCorrectThread()) {
      CPAs.closeCpaIfPossible(analysis.getDssBlockAnalysis().getDcpa(), logger);
      closed = true;
    }
  }
}
