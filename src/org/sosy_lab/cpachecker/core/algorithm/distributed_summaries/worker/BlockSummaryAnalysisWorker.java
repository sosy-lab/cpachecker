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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
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
    /*
    addTimer(forwardAnalysis);
    addTimer(backwardAnalysis);

    stats.forwardTimer.register(forwardAnalysisTime);
    stats.backwardTimer.register(backwardAnalysisTime);*/
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
        return ImmutableSet.of();
      case ERROR_CONDITION_UNREACHABLE:
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
        forwardAnalysis.getDistributedCPA().getProceedOperator().proceed(message);
    if (processing.end()) {
      return processing;
    }
    return performForwardAnalysis(processing);
  }

  private Collection<BlockSummaryMessage> processErrorCondition(BlockSummaryMessage message)
      throws SolverException, InterruptedException, CPAException {
    DistributedCompositeCPA distributed = backwardAnalysis.getDistributedCPA();
    BlockSummaryMessageProcessing processing = distributed.getProceedOperator().proceed(message);
    if (processing.end()) {
      forwardAnalysis
          .getDistributedCPA()
          .updateErrorCondition((BlockSummaryErrorConditionMessage) message);
      Collection<? extends BlockSummaryMessage> forwardUpdates =
          FluentIterable.from(performForwardAnalysis(latestPreconditions))
              .filter(BlockSummaryPostConditionMessage.class)
              .toSet();
      return ImmutableSet.<BlockSummaryMessage>builder()
          .addAll(processing)
          .addAll(forwardUpdates)
          .build();
    }
    return performBackwardAnalysisWithAbstraction(processing);
  }

  private Collection<BlockSummaryMessage> performForwardAnalysis(
      Collection<BlockSummaryMessage> pPostConditionMessages)
      throws CPAException, InterruptedException, SolverException {
    latestPreconditions.clear();
    latestPreconditions.addAll(pPostConditionMessages);
    forwardAnalysis.getDistributedCPA().synchronizeKnowledge(backwardAnalysis.getDistributedCPA());
    forwardAnalysisTime.start();
    Collection<BlockSummaryMessage> response = forwardAnalysis.analyze(pPostConditionMessages);
    forwardAnalysisTime.stop();
    return response;
  }

  private Collection<BlockSummaryMessage> performBackwardAnalysis(
      BlockSummaryMessageProcessing pMessageProcessing)
      throws CPAException, InterruptedException, SolverException {
    Preconditions.checkArgument(
        pMessageProcessing.size() == 1, "BackwardAnalysis can only be based on one message");
    backwardAnalysis.getDistributedCPA().synchronizeKnowledge(forwardAnalysis.getDistributedCPA());
    backwardAnalysisTime.start();
    Collection<BlockSummaryMessage> result = backwardAnalysis.analyze(pMessageProcessing);
    backwardAnalysisTime.stop();
    return result;
  }

  private Collection<BlockSummaryMessage> performBackwardAnalysisWithAbstraction(
      BlockSummaryMessageProcessing pMessageProcessing)
      throws InterruptedException, CPAException, SolverException {
    Preconditions.checkArgument(pMessageProcessing.size() == 1);
    BlockSummaryErrorConditionMessage msg =
        (BlockSummaryErrorConditionMessage) Iterables.getOnlyElement(pMessageProcessing);
    if (msg.getBlockId().equals(getBlockId())
        && msg.getTargetNodeNumber() != block.getLastNode().getNodeNumber()) {
      return performBackwardAnalysis(pMessageProcessing);
    }
    backwardAnalysis.getDistributedCPA().updateErrorCondition(msg);
    backwardAnalysis.getDistributedCPA().synchronizeKnowledge(forwardAnalysis.getDistributedCPA());
    Collection<BlockSummaryMessage> result = forwardAnalysis.analyze(latestPreconditions);
    ImmutableSet<? extends BlockSummaryMessage> answer =
        FluentIterable.from(result).filter(BlockSummaryPostConditionMessage.class).toSet();
    // TODO: if abstraction -> broadcast. If not -> don't.
    if (!answer.isEmpty()) {
      Collection<BlockSummaryMessage> backwardResult = performBackwardAnalysis(pMessageProcessing);
      return ImmutableSet.<BlockSummaryMessage>builder()
          .addAll(answer)
          .addAll(backwardResult)
          .build();
    }
    return ImmutableSet.of(
        BlockSummaryMessage.newErrorConditionUnreachableMessage(block.getId(), "forward failed"));
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

  /*  private void addTimer(BlockAnalysis pBlockAnalysis) {
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedSerializeTime, StatTimerType.SERIALIZE);
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedDeserializeTime, StatTimerType.DESERIALIZE);
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedForwardTime, StatTimerType.PROCEED_F);
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedBackwardTime, StatTimerType.PROCEED_B);
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedCombineTime, StatTimerType.COMBINE);
  }*/

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + shutdownRequested() + '}';
  }

  public ForwardBlockAnalysis getForwardAnalysis() {
    return forwardAnalysis;
  }
}
