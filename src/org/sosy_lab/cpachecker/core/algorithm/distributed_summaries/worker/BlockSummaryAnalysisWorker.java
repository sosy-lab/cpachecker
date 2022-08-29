// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.BackwardBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.ForwardBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
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

  private final ForwardBlockAnalysis forwardAnalysis;
  private final BackwardBlockAnalysis backwardAnalysis;

  private final Collection<BlockSummaryMessage> latestPreconditions;

  private boolean shutdown;

  private final BlockSummaryConnection connection;

  private final StatTimer forwardAnalysisTime = new StatTimer("Forward Analysis");
  private final StatTimer backwardAnalysisTime = new StatTimer("Backward Analysis");
  private final StatTimer backwardAnalysisAbstractionTime =
      new StatTimer("Backward Analysis Abstraction");

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
    Configuration backwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getBackwardConfiguration()).build();

    Specification backwardSpecification =
        Specification.fromFiles(
            ImmutableSet.of(
                Path.of("config/specification/MainEntry.spc"),
                Path.of("config/specification/TerminatingFunctions.spc")),
            pCFA,
            backwardConfiguration,
            getLogger(),
            pShutdownManager.getNotifier());

    backwardAnalysis =
        new BackwardBlockAnalysis(
            getLogger(),
            pBlock,
            pCFA,
            backwardSpecification,
            backwardConfiguration,
            pShutdownManager,
            pOptions);

    forwardAnalysis =
        new ForwardBlockAnalysis(
            getLogger(),
            pBlock,
            pCFA,
            pSpecification,
            forwardConfiguration,
            pShutdownManager,
            pOptions);

    latestPreconditions = new LinkedHashSet<>();
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage message)
      throws InterruptedException, CPAException, IOException, SolverException {
    switch (message.getType()) {
      case ERROR_CONDITION:
        return processErrorCondition(message);
      case BLOCK_POSTCONDITION:
        return processBlockPostCondition(message);
      case ERROR:
        // fall through
      case FOUND_RESULT:
        shutdown = true;
        return ImmutableSet.of(BlockSummaryMessage.newStatisticsMessage(getBlockId(), getStats()));
      case ERROR_CONDITION_UNREACHABLE:
        // fall through
      case STATISTICS:
        return ImmutableSet.of();
      default:
        throw new AssertionError("MessageType " + message.getType() + " does not exist");
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

  private Collection<BlockSummaryMessage> processBlockPostCondition(BlockSummaryMessage message)
      throws CPAException, InterruptedException, SolverException {
    BlockSummaryMessageProcessing processing =
        forwardAnalysis.getAnalysis().getProceedOperator().proceed(message);
    if (processing.end()) {
      return processing;
    }
    return performForwardAnalysis(processing);
  }

  private Collection<BlockSummaryMessage> processErrorCondition(BlockSummaryMessage message)
      throws SolverException, InterruptedException, CPAException {
    Preconditions.checkArgument(
        message instanceof BlockSummaryErrorConditionMessage,
        "Method processErrorCondition can only process messages of type  %s",
        BlockSummaryErrorConditionMessage.class);
    DistributedCompositeCPA distributed = backwardAnalysis.getAnalysis();
    BlockSummaryMessageProcessing processing = distributed.getProceedOperator().proceed(message);
    if (processing.end()) {
      forwardAnalysis
          .getAnalysis()
          .updateErrorCondition((BlockSummaryErrorConditionMessage) message);
      Collection<BlockSummaryPostConditionMessage> filteredForward =
          FluentIterable.from(forwardAnalysis.analyze(latestPreconditions))
              .filter(BlockSummaryPostConditionMessage.class)
              .toSet();
      return ImmutableSet.<BlockSummaryMessage>builder()
          .addAll(processing)
          .addAll(filteredForward)
          .build();
    }
    return performBackwardAnalysisWithAbstraction(processing);
  }

  private Collection<BlockSummaryMessage> performForwardAnalysis(
      Collection<BlockSummaryMessage> pPostConditionMessages)
      throws CPAException, InterruptedException, SolverException {
    try {
      forwardAnalysisTime.start();
      latestPreconditions.clear();
      latestPreconditions.addAll(pPostConditionMessages);
      forwardAnalysis.getAnalysis().synchronizeKnowledge(backwardAnalysis.getAnalysis());
      return forwardAnalysis.analyze(pPostConditionMessages);
    } finally {
      forwardAnalysisTime.stop();
    }
  }

  private Collection<BlockSummaryMessage> performBackwardAnalysis(
      BlockSummaryMessageProcessing pMessageProcessing)
      throws CPAException, InterruptedException, SolverException {
    try {
      backwardAnalysisTime.start();
      Preconditions.checkArgument(
          pMessageProcessing.size() == 1, "BackwardAnalysis can only be based on one message");
      backwardAnalysis.getAnalysis().synchronizeKnowledge(forwardAnalysis.getAnalysis());
      return backwardAnalysis.analyze(pMessageProcessing);
    } finally {
      backwardAnalysisTime.stop();
    }
  }

  private Collection<BlockSummaryMessage> performBackwardAnalysisWithAbstraction(
      BlockSummaryMessageProcessing pMessageProcessing)
      throws InterruptedException, CPAException, SolverException {
    Preconditions.checkArgument(pMessageProcessing.size() == 1);
    try {
      backwardAnalysisAbstractionTime.start();
      BlockSummaryErrorConditionMessage msg =
          (BlockSummaryErrorConditionMessage) Iterables.getOnlyElement(pMessageProcessing);
      if (msg.getBlockId().equals(getBlockId())
          && msg.getTargetNodeNumber() != block.getLastNode().getNodeNumber()) {
        return performBackwardAnalysis(pMessageProcessing);
      }

      DistributedCompositeCPA forwardBlockDCPA = forwardAnalysis.getAnalysis();
      forwardBlockDCPA.updateErrorCondition(msg);
      Collection<? extends BlockSummaryMessage> forwardUpdates =
          FluentIterable.from(performForwardAnalysis(latestPreconditions))
              .filter(BlockSummaryPostConditionMessage.class)
              .toSet();

      if (!forwardUpdates.isEmpty()) {
        backwardAnalysis.getAnalysis().updateErrorCondition(msg);
        backwardAnalysis.getAnalysis().synchronizeKnowledge(forwardAnalysis.getAnalysis());
        Collection<BlockSummaryMessage> backwardResult =
            performBackwardAnalysis(pMessageProcessing);
        return ImmutableSet.<BlockSummaryMessage>builder()
            .addAll(forwardUpdates)
            .addAll(backwardResult)
            .build();
      }
      return ImmutableSet.of();
    } finally {
      backwardAnalysisAbstractionTime.stop();
    }
  }

  @Override
  public void run() {
    try {
      broadcast(forwardAnalysis.performInitialAnalysis());
      super.run();
    } catch (CPAException pE) {
      getLogger().logException(Level.SEVERE, pE, "Worker stopped working...");
      broadcastOrLogException(
          ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getBlockId(), pE)));
    } catch (InterruptedException pE) {
      getLogger().logException(Level.SEVERE, pE, "Thread interrupted unexpectedly.");
    } catch (SolverException pE) {
      getLogger().logException(Level.SEVERE, pE, "Solver ran into an error.");
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + shutdownRequested() + '}';
  }

  public ForwardBlockAnalysis getForwardAnalysis() {
    return forwardAnalysis;
  }

  private Map<String, Object> getStats() {
    return ImmutableMap.<String, Object>builder()
        .put(
            BlockSummaryStatisticType.FORWARD_TIME.name(),
            forwardAnalysisTime.getConsumedTime().asNanos())
        .put(
            BlockSummaryStatisticType.BACKWARD_TIME.name(),
            backwardAnalysisTime.getConsumedTime().asNanos())
        .put(
            BlockSummaryStatisticType.BACKWARD_ABSTRACTION_TIME.name(),
            backwardAnalysisAbstractionTime.getConsumedTime().asNanos())
        .put(BlockSummaryStatisticType.MESSAGES_SENT.name(), Integer.toString(getSentMessages()))
        .put(
            BlockSummaryStatisticType.MESSAGES_RECEIVED.name(),
            Integer.toString(getReceivedMessages()))
        .put(
            BlockSummaryStatisticType.FORWARD_ANALYSIS_STATS.name(),
            forwardAnalysis.getAnalysis().getStatistics().getStatistics())
        .put(
            BlockSummaryStatisticType.BACKWARD_ANALYSIS_STATS.name(),
            backwardAnalysis.getAnalysis().getStatistics().getStatistics())
        .buildOrThrow();
  }
}
