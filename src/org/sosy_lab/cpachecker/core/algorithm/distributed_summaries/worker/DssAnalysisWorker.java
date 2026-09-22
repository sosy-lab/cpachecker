// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssAllWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssMessageBroadcaster;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DeserializeBlockStateOperator;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;
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

  private final DssMessageFactory messageFactory;

  private final DssConnection connection;

  private final DssSingleWorkerStatistics workerStats;

  private boolean shutdown;
  private boolean closed;

  /** Whether a stored postcondition still owes an exploration, see {@link #processMessage}. */
  private boolean preconditionsPending;

  /**
   * A successor whose stored violation conditions still owe an exploration, or {@code null} if none
   * does, see {@link #processMessage}.
   */
  private @Nullable String pendingViolationConditionSender;

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
      DssAllWorkerStatistics pWorkerStatistics,
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

  /**
   * Stores what a message carries and explores the block once the worker's queue has run empty.
   *
   * <p>Exploring after every single message is what makes the multithreaded execution expensive. A
   * worker is usually handed a burst of messages: the block is explored from the first one, and the
   * result is superseded by the second before anyone reads it. Storing the whole burst first and
   * exploring once afterwards produces the same conditions with a fraction of the analyses.
   *
   * <p>The exploration cannot simply be left to the next message, because there may be no next
   * message. It has to happen before this worker blocks on its queue again, since {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssThreadMonitor} reads a
   * worker waiting on an empty queue as a worker with nothing left to do, and would report a
   * verdict while an exploration is still owed.
   */
  @Override
  public Collection<DssMessage> processMessage(DssMessage message) {
    Collection<DssMessage> messages = store(message);
    if (shutdown || !isAnalysisPending() || getConnection().hasPendingMessages()) {
      return messages;
    }
    try {
      return ImmutableList.<DssMessage>builder().addAll(messages).addAll(analyzePending()).build();
    } catch (Exception | Error e) {
      return ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e));
    }
  }

  private boolean isAnalysisPending() {
    return preconditionsPending || pendingViolationConditionSender != null;
  }

  /**
   * Runs the exploration that the stored messages owe.
   *
   * <p>A single exploration covers both kinds of update, because it reads everything the two
   * handlers hold rather than only what the message that triggered it brought. The backward variant
   * is preferred when a violation condition is owed: it is the one that still explores a block all
   * of whose predecessors reported an unreachable block end, which is exactly what a successor
   * asking about that block needs.
   */
  private Collection<DssMessage> analyzePending()
      throws CPAException, InterruptedException, SolverException {
    String violationConditionSender = pendingViolationConditionSender;
    preconditionsPending = false;
    pendingViolationConditionSender = null;
    return violationConditionSender == null
        ? analysis.getDssBlockAnalysis().analyzePreconditions()
        : analysis.getDssBlockAnalysis().analyzeViolationConditions(violationConditionSender);
  }

  private Collection<DssMessage> store(DssMessage message) {
    return switch (message.getType()) {
      case POST_CONDITION -> {
        try {
          DssMessageProcessing processing =
              analysis.getDssBlockAnalysis().storePrecondition((DssPostConditionMessage) message);
          if (!processing.shouldProceed()) {
            yield processing;
          }
          preconditionsPending = true;
          yield ImmutableSet.of();
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
          pendingViolationConditionSender = message.getSenderId();
          yield ImmutableSet.of();
        } catch (Exception | Error e) {
          yield ImmutableSet.of(messageFactory.createDssExceptionMessage(getBlockId(), e));
        }
      }
      case EXCEPTION -> {
        shutdown = true;
        yield ImmutableSet.of();
      }
      case RESULT -> {
        shutdown = true;
        if (message.getResult() == Result.TRUE) {
          yield ImmutableSet.of(
              messageFactory.createDssCorrectnessWitnessMessage(
                  getBlockId(), analysis.getDssBlockAnalysis().serializedPreconditions()));
        }
        yield ImmutableSet.of();
      }
      case WITNESS -> ImmutableSet.of();
    };
  }

  @CanIgnoreReturnValue
  public DssMessageProcessing storeMessage(DssMessage message)
      throws SolverException, InterruptedException, CPAException {
    return switch (message.getType()) {
      case RESULT, EXCEPTION, WITNESS -> DssMessageProcessing.stop();
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
      switch (message.getType()) {
        case POST_CONDITION -> {
          broadcaster.broadcastToObserver(message);
          broadcaster.broadcastToIds(message, block.getSuccessorIds());
        }
        case VIOLATION_CONDITION -> {
          if (block.getPredecessorIds().isEmpty()) {
            String violationPathString = message.extractBlockStateWitnessString();
            SegmentedPaths violationPath =
                DeserializeBlockStateOperator.parseWitness(violationPathString).witness();
            broadcaster.broadcastToAll(
                messageFactory.createDssResultMessage(getId(), Result.FALSE));
            broadcaster.broadcastToAll(
                messageFactory.createDssViolationWitnessMessage(getId(), violationPath));
          } else {
            broadcaster.broadcastToObserver(message);
            broadcaster.broadcastToIds(message, block.getPredecessorIds());
          }
        }
        case EXCEPTION, RESULT, WITNESS -> {
          // the worker will also broadcast to itself and react
          // appropriately in processMessage
          broadcaster.broadcastToAll(message);
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
    if (!closed && analysis.wouldBeCalledFromCorrectThread()) {
      CPAs.closeCpaIfPossible(analysis.getDssBlockAnalysis().getDcpa(), logger);
      closed = true;
    }
  }
}
